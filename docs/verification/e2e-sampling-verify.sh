#!/usr/bin/env bash
# =====================================================================
# 现场采样关键状态流转 + 任务归属(越权)端到端验证
# 前置: 后端已启动(默认 http://localhost:8080/api), MySQL 已按 deploy/mysql/init 初始化
# 用法: bash docs/verification/e2e-sampling-verify.sh [BASE_URL]
# =====================================================================
set -u
BASE="${1:-http://localhost:8080/api}"
PASS=0; FAIL=0

# ---- JSON 小工具 ----
jget() { python3 -c "import sys,json;d=json.load(sys.stdin);print(eval('d'+sys.argv[1]))" "$1" 2>/dev/null; }
jhas() { python3 -c "import sys,json;d=json.load(sys.stdin);print('yes' if eval(sys.argv[1]) else 'no')" "$1" 2>/dev/null; }

check() { # check <描述> <实际> <期望>
  if [ "$2" = "$3" ]; then PASS=$((PASS+1)); echo "  ✔ $1 ($2)";
  else FAIL=$((FAIL+1)); echo "  ✘ $1 期望[$3] 实际[$2]"; fi
}

req() { # req <METHOD> <PATH> <TOKEN> [BODY]
  local M="$1" P="$2" T="$3" B="${4:-}"
  if [ -n "$B" ]; then
    curl -s -X "$M" "$BASE$P" -H "Content-Type: application/json" -H "Authorization: Bearer $T" -d "$B"
  else
    curl -s -X "$M" "$BASE$P" -H "Authorization: Bearer $T"
  fi
}

login() { # login <user> -> token
  curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"admin123\"}" | jget "['data']['token']"
}

echo "== 0. 登录(admin/sampler/samplemgr/sales/reviewer/manager) =="
T_ADMIN=$(login admin);   T_SAMPLER=$(login sampler); T_MGR=$(login samplemgr)
T_SALES=$(login sales);   T_REVIEW=$(login reviewer); T_MANAGER=$(login manager)
for v in T_ADMIN T_SAMPLER T_MGR T_SALES T_REVIEW T_MANAGER; do
  eval "val=\$$v"; check "登录 $v" "$([ -n "$val" ] && [ "$val" != None ] && echo ok || echo EMPTY)" ok
done

echo "== 1. 委托单 DRAFT -> REVIEWING -> ACCEPTED =="
R=$(req POST /entrusts "$T_ADMIN" '{"title":"E2E验证-地表水委托","customerId":1001,"entrustType":"ENTRUST","urgency":"NORMAL","contactPerson":"陈工","contactPhone":"13900001111","expectedReportDate":"2026-09-30","items":[{"itemName":"pH值","standardCode":"HJ 1147-2020","standardName":"水质 pH值的测定 电极法","sampleName":"地表水","sampleQty":2,"qty":2,"unitPrice":100,"amount":200,"sortNo":1}],"points":[{"name":"厂区总排口","lng":118.78,"lat":32.06,"addrDesc":"江宁区科学园","sortNo":1}]}')
OID=$(echo "$R" | jget "['data']")
echo "  委托单ID=$OID"
check "创建委托单" "$([ -n "$OID" ] && [ "$OID" != None ] && echo ok || echo FAIL:$R)" ok
check "初始状态DRAFT" "$(req GET /entrusts/$OID "$T_ADMIN" | jget "['data']['order']['status']")" DRAFT
req POST /entrusts/$OID/submit-review "$T_ADMIN" '{"remark":"提交评审"}' >/dev/null
check "提交评审后REVIEWING" "$(req GET /entrusts/$OID "$T_ADMIN" | jget "['data']['order']['status']")" REVIEWING
# 评审节点1: 合同评审员
TID1=$(req GET /approval/todo "$T_REVIEW" | python3 -c "import sys,json;d=json.load(sys.stdin);print(next(t['id'] for t in d['data'] if t.get('bizId')==$OID))" 2>/dev/null)
req POST /approval/act "$T_REVIEW" "{\"taskId\":$TID1,\"approve\":true,\"comment\":\"资质能力符合\"}" >/dev/null
# 评审节点2: 市场主管
TID2=$(req GET /approval/todo "$T_MANAGER" | python3 -c "import sys,json;d=json.load(sys.stdin);print(next(t['id'] for t in d['data'] if t.get('bizId')==$OID))" 2>/dev/null)
req POST /approval/act "$T_MANAGER" "{\"taskId\":$TID2,\"approve\":true,\"comment\":\"同意受理\"}" >/dev/null
check "两级评审通过ACCEPTED" "$(req GET /entrusts/$OID "$T_ADMIN" | jget "['data']['order']['status']")" ACCEPTED

echo "== 2. 采样计划制定/下发/派工 =="
R=$(req POST /sampling/plans "$T_ADMIN" "{\"orderId\":$OID,\"title\":\"E2E采样计划\",\"planDate\":\"2026-09-13\",\"weather\":\"晴\"}")
PID=$(echo "$R" | jget "['data']")
check "创建计划" "$([ -n "$PID" ] && [ "$PID" != None ] && echo ok || echo FAIL:$R)" ok
req POST /sampling/plans/$PID/issue "$T_ADMIN" >/dev/null
check "计划下发ISSUED" "$(req GET /sampling/plans/$PID "$T_ADMIN" | jget "['data']['plan']['status']")" ISSUED
R=$(req POST /sampling/tasks/assign "$T_ADMIN" "{\"planId\":$PID,\"assigneeId\":6,\"remark\":\"派给钱采样员\"}")
TASK=$(echo "$R" | jget "['data']")
check "派工生成任务" "$([ -n "$TASK" ] && [ "$TASK" != None ] && echo ok || echo FAIL:$R)" ok
check "任务ASSIGNED" "$(req GET /sampling/plans/$PID/tasks "$T_ADMIN" | jget "['data'][0]['status']")" ASSIGNED

echo "== 3. 移动端: 我的任务/详情/下载/样品提交 =="
check "我的任务含新任务" "$(req GET "/mobile/sampling/my-tasks?current=1&size=20" "$T_SAMPLER" | jhas "any(t['id']==$TASK for t in d['data']['records'])")" yes
check "详情(本人)200" "$(req GET /mobile/sampling/tasks/$TASK "$T_SAMPLER" | jget "['code']")" 200
req POST /mobile/sampling/tasks/$TASK/download "$T_SAMPLER" >/dev/null
check "下载时间已记录" "$(req GET /sampling/plans/$PID/tasks "$T_ADMIN" | jhas "d['data'][0]['downloadedAt'] is not None")" yes
SAMPLE_BODY="{\"clientUuid\":\"e2e-uuid-0001\",\"taskId\":$TASK,\"sampleName\":\"地表水\",\"itemName\":\"pH值\",\"temperature\":21.5,\"ph\":7.2,\"lng\":118.78,\"lat\":32.06,\"storageCondition\":\"冷藏\"}"
R=$(req POST /mobile/sampling/samples "$T_SAMPLER" "$SAMPLE_BODY")
SID=$(echo "$R" | jget "['data']['id']"); SCODE=$(echo "$R" | jget "['data']['sampleCode']")
check "提交样品COLLECTED" "$(echo "$R" | jget "['data']['status']")" COLLECTED
check "首件样品推进SAMPLING" "$(req GET /entrusts/$OID "$T_ADMIN" | jget "['data']['order']['status']")" SAMPLING
R2=$(req POST /mobile/sampling/samples "$T_SAMPLER" "$SAMPLE_BODY")
check "重复提交(同clientUuid)幂等返回同一样品" "$(echo "$R2" | jget "['data']['id']")" "$SID"
check "任务下样品数仍为1" "$(req GET /mobile/sampling/tasks/$TASK "$T_SAMPLER" | jhas "len(d['data']['samples'])==1")" yes

echo "== 4. 越权访问(非任务采样员 sales 登录) =="
check "越权读详情被拒403" "$(req GET /mobile/sampling/tasks/$TASK "$T_SALES" | jget "['code']")" 403
check "越权下载确认被拒403" "$(req POST /mobile/sampling/tasks/$TASK/download "$T_SALES" | jget "['code']")" 403
check "越权提交样品被拒403" "$(req POST /mobile/sampling/samples "$T_SALES" "{\"clientUuid\":\"e2e-uuid-evil\",\"taskId\":$TASK}" | jget "['code']")" 403
check "越权提交任务被拒403" "$(req POST /mobile/sampling/tasks/$TASK/submit "$T_SALES" '{"remark":"x"}' | jget "['code']")" 403
check "越权发起交接被拒403" "$(req POST /mobile/sampling/handovers "$T_SALES" "{\"taskId\":$TASK}" | jget "['code']")" 403
check "越权扫码查询被拒403" "$(req GET "/mobile/sampling/samples/$SCODE" "$T_SALES" | jget "['code']")" 403
check "样品管理员扫码可查200" "$(req GET "/mobile/sampling/samples/$SCODE" "$T_MGR" | jget "['code']")" 200

echo "== 5. 任务提交 + 样品交接 =="
check "采样员提交任务" "$(req POST /mobile/sampling/tasks/$TASK/submit "$T_SAMPLER" '{"remark":"现场完成"}' | jget "['code']")" 200
check "任务SUBMITTED" "$(req GET /sampling/plans/$PID/tasks "$T_ADMIN" | jget "['data'][0]['status']")" SUBMITTED
R=$(req POST /mobile/sampling/handovers "$T_SAMPLER" "{\"taskId\":$TASK,\"sampleStatus\":\"完好\"}")
HID=$(echo "$R" | jget "['data']")
check "发起交接" "$([ -n "$HID" ] && [ "$HID" != None ] && echo ok || echo FAIL:$R)" ok
check "重复发起交接被拒" "$(req POST /mobile/sampling/handovers "$T_SAMPLER" "{\"taskId\":$TASK}" | jget "['message']" | grep -c "重复")" 1
check "交接单PENDING" "$(req GET /sampling/handovers/$HID "$T_MGR" | jget "['data']['handover']['status']")" PENDING
check "样品管理员确认接收" "$(req POST /sampling/handovers/$HID/confirm "$T_MGR" '{"remark":"核对无误"}' | jget "['code']")" 200
check "交接单CONFIRMED" "$(req GET /sampling/handovers/$HID "$T_MGR" | jget "['data']['handover']['status']")" CONFIRMED
check "样品RECEIVED" "$(req GET /sampling/handovers/$HID "$T_MGR" | jget "['data']['samples'][0]['sample']['status']")" RECEIVED
check "任务HANDED" "$(req GET /sampling/plans/$PID/tasks "$T_ADMIN" | jget "['data'][0]['status']")" HANDED
check "委托单推进TESTING" "$(req GET /entrusts/$OID "$T_ADMIN" | jget "['data']['order']['status']")" TESTING
check "已交接任务禁止再提交样品3001" "$(req POST /mobile/sampling/samples "$T_SAMPLER" "{\"clientUuid\":\"e2e-uuid-late\",\"taskId\":$TASK}" | jget "['code']")" 3001

echo "== 6. 委托单状态时间线 =="
req GET /entrusts/$OID/timeline "$T_ADMIN" | python3 -c "
import sys,json
for t in json.load(sys.stdin)['data']:
    print('  ', t.get('fromStatus','-'), '->', t.get('toStatus','-'), '(%s)' % t.get('action',''), t.get('operatorName',''))"

echo
echo "==================== 结果: 通过 $PASS 项, 失败 $FAIL 项 ===================="
[ "$FAIL" = 0 ]
