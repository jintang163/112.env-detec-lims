# 112f2 现场采样 — 交付验证记录

> 对应审查意见 `docs/TRAe-advice-112f2.md` 的修复项 1/2/3。本记录只写已实际运行验证的内容，未验证项如实标注。
> 验证时间：2026-09-13；验证环境：便携工具链（Temurin JDK 17.0.20.1、Maven 3.9.9、MySQL 8.0.39、Redis 7.2.5 源码编译），均在本机用户目录运行，未依赖 Docker。

## 1. 任务归属校验（已实现 + 已验证）

统一在 `SecurityUtils.checkOwnerOrAdmin(ownerId, message)` 执行"当前用户 == 资源归属人，或 ROLE_ADMIN"，否则抛 403。

| 接口 | 改动位置 | 校验 |
| --- | --- | --- |
| GET /mobile/sampling/tasks/{id} 任务详情 | `SamplingTaskService.mobileDetail`（控制器由 `detail` 切换为 `mobileDetail`） | 仅任务采样员/管理员 |
| POST /mobile/sampling/tasks/{id}/download 下载确认 | `FieldSampleService.markDownloaded` | 仅任务采样员/管理员 |
| POST /mobile/sampling/samples 样品提交 | `FieldSampleService.submit`（事务内、幂等判断之前） | 仅任务采样员/管理员；样品责任人仍记录为任务采样员 |
| POST /mobile/sampling/tasks/{id}/submit 任务提交 | `FieldSampleService.markTaskSubmitted` | 仅任务采样员/管理员 |
| POST /mobile/sampling/handovers 发起交接 | `HandoverService.create`（原有内联校验重构为统一助手，行为不变） | 仅任务采样员/管理员 |
| GET /mobile/sampling/samples/{code} 扫码核验 | `FieldSampleService.getVOByCode` | 样品管理员可查任意样品（接收场景）；其余登录用户仅限本人所采 |

PC 端 `/sampling/tasks/{id}` 详情沿用原 `detail`，行为不变。

## 2. 单元测试（已实现 + 已验证，23 项全绿）

新增 `lims-backend/src/test/`：

- `FieldSampleServiceTest`（13 项）：提交越权 403、同 clientUuid 重复提交幂等返回已有样品且不重复 insert、采样员正常提交（COLLECTED + 首件推进 START_SAMPLING）、管理员可代提交、已交接任务禁止提交（3001）、任务提交越权/状态非法/正常、下载确认越权/首次记录/重复幂等、扫码越权 403、样品管理员可查。
- `HandoverServiceTest`（7 项）：发起越权 403、无可交接样品拦截、已有待接收交接单重复发起拦截、正常发起（PENDING + 样品绑定 + 通知）、非待接收确认拦截、接收确认（CONFIRMED + 样品 RECEIVED + 任务 HANDED + 委托单 START_TESTING）、拒收（REJECTED + 释放样品 + 任务退回 ASSIGNED）。
- `SamplingTaskServiceTest`（3 项）：移动端详情越权 403、本人可见、管理员可见。

```
Tests run: 13, Failures: 0, Errors: 0 - FieldSampleServiceTest
Tests run: 7,  Failures: 0, Errors: 0 - HandoverServiceTest
Tests run: 3,  Failures: 0, Errors: 0 - SamplingTaskServiceTest
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0   [INFO] BUILD SUCCESS
```

## 3. 后端编译（已验证）

```
mvn -B -DskipTests compile   → [INFO] BUILD SUCCESS（Total time: 02:10 min）
mvn -B test                  → [INFO] BUILD SUCCESS（23 项测试全绿）
mvn -B -DskipTests package   → target/lims-backend.jar（84 MB）
```

## 4. 数据库初始化（已验证）

MySQL 8.0.39 按 `deploy/mysql/init` 顺序执行：

```
mysql < 01-schema.sql → SCHEMA_OK
mysql < 02-data.sql   → DATA_OK
information_schema 统计 lims 库表数 = 41（与脚本 CREATE TABLE 数一致）
sys_user 7 个账号（admin/manager/sales/reviewer/finance/sampler/samplemgr），sys_role 7 个角色
```

## 5. 关键状态流转端到端验证（已验证，39 项全过）

脚本：`docs/verification/e2e-sampling-verify.sh`（可重复执行）；完整输出：`docs/verification/e2e-result.txt`。
流程：登录 6 个角色 → 建委托单 → 提交评审 → 两级审批 → 采样计划制定/下发/派工 → 移动端我的任务/详情/下载/提交样品（含幂等重传）→ 越权访问 6 项 → 任务提交 → 发起交接（含重复发起拦截）→ 样品管理员接收确认 → 状态与时间线核验。

```
==================== 结果: 通过 39 项, 失败 0 项 ====================
```

委托单状态时间线（接口 `/entrusts/{id}/timeline` 实测返回）：

```
-         -> DRAFT     (CREATE)         系统管理员
DRAFT     -> REVIEWING (SUBMIT)         系统管理员
REVIEWING -> ACCEPTED  (REVIEW_APPROVE) 李经理(市场主管)
ACCEPTED  -> SAMPLING  (START_SAMPLING) 钱采样员        ← 首件样品采集自动推进
SAMPLING  -> TESTING   (START_TESTING)  孔样品管理员    ← 交接接收确认自动推进
```

配套状态：任务 ASSIGNED→SUBMITTED→HANDED；样品 COLLECTED→RECEIVED；交接 PENDING→CONFIRMED；越权访问（非任务采样员读详情/下载/提交样品/提交任务/发起交接/扫码）全部 403。

## 6. 需求-验证矩阵

| 审查修复项 | 状态 | 证据 |
| --- | --- | --- |
| 移动端详情/下载/样品提交/任务提交/交接 归属校验 | 已实现+已验证 | 单测 §2 + E2E 第 4 节 6 项 403 |
| 样品提交校验当前用户==任务采样员；重复提交与越权测试 | 已实现+已验证 | `FieldSampleService.submit`；单测 13 项 + E2E 幂等项 |
| 后端编译验收 | 已验证 | §3 |
| 数据库初始化验收 | 已验证 | §4 |
| 关键状态流转验收 | 已验证 | §5（39 项） |

## 7. 如实记录的遗留观察

- **单号序列与 SQL 种子数据需对齐**：`CodeGenerator` 用 Redis INCR 从 1 发号，而 `02-data.sql` 直接插入了 WT-2026-0001 等单号。全新部署（Redis 为空）首次建单会撞 `uk_code` 唯一键。本次验证按种子最大值预置了 `lims:seq:*` 计数器后通过。建议后续在 `02-data.sql` 末尾补序列初始化说明，或让发号器冲突时自增重试（未改动产品代码，仅记录）。
- 设备领用归还的库存回补逻辑本次未做端到端验证（不在本次修复范围），仅交接/样品/任务/委托单链路已验证。
- 本机验证进程：MySQL（socket `~/mysql-run/mysql.sock`，端口 3306）、Redis（6379）、后端（8080，`~/lims-backend.log`）。停止：`kill $(cat ~/mysql-run/mysqld.pid)`、`~/tools/redis-7.2.5/src/redis-cli shutdown`、`pkill -f lims-backend.jar`。
