# 阶段一接口文档（委托与合同管理）

- Base URL: `/api`
- 认证: 除登录/白名单外，请求头 `Authorization: Bearer <token>`
- 统一返回: `{ code:200, message, data, timestamp }`，分页 `data: { records,total,current,size }`
- WebSocket: `/api/ws/notify?token=<jwt>`，推送消息 `{type:"NOTIFICATION|TODO",title,content,bizType,bizId,time}`

## 认证 / 系统

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /auth/login | 登录，返回 JWT 与角色权限 |
| POST | /auth/logout | 退出（清除 Redis 权限快照） |
| GET | /auth/info | 当前用户、菜单树、权限 |
| GET | /dict/{code} | 字典项（entrust_status/contract_status/quote_status…） |
| GET | /system/users/options?roleCode= | 用户下拉（客户分配） |
| POST | /files/upload | 通用上传（bizType,bizId） |
| GET | /files/local/{objectName} | 本地存储文件读取（OnlyOffice 白名单） |
| GET | /notifications | 通知分页 |
| GET | /notifications/unread-count | 未读数 |
| POST | /notifications/{id}/read、/notifications/read-all | 已读 |
| GET | /system/operation-logs | 操作日志分页（仅管理员；写操作由 AOP 切面自动记录，参数 module/username/keyword） |

## 工作台

- GET /dashboard/summary 汇总指标与回款
- GET /dashboard/entrust-status 委托状态分布
- GET /dashboard/expiring-entrusts 7天内到期
- GET /dashboard/qualification-warnings 资质临期/过期

## 客户 /customers

- GET /customers?scope=all|mine|pool&keyword&customerLevel&customerType&status
- GET /customers/{id}；POST /customers（新增/编辑）
- POST /customers/{id}/claim 认领公海
- POST /customers/{id}/release 退回公海；POST /customers/{id}/transfer 分配/转交
- POST /customers/{id}/status 停用(0)/恢复(1)
- GET /customers/{id}/pool-logs 公海流转记录
- POST /customers/credit/adjust 信用调整（changeType 1分/2额度/3账期，自动联动分级）
- GET /customers/{id}/credit-logs
- GET/POST /customers/{id}/qualifications、POST /customers/qualifications、DELETE /customers/qualifications/{id}
- GET /customers/{id}/follows、POST /customers/follows

## 合同 /contracts

- GET /contracts；GET /contracts/{id}；POST /contracts（草稿）
- POST /contracts/{id}/submit 提交两级审批（市场主管→总经理）
- POST /contracts/{id}/status 业务推进（EXECUTING/COMPLETED/TERMINATED）
- GET /contracts/{id}/performance 回款/开票汇总
- GET /contracts/{id}/payments?payType=1计划/2收款/3开票
- POST /contracts/payments；DELETE /contracts/payments/{id}
- POST /contracts/changes 发起变更（两级审批，金额变更通过后自动同步）
- GET /contracts/{id}/changes
- GET /onlyoffice/editor-config/CONTRACT/{id}?edit=true OnlyOffice 编辑器配置
- POST /onlyoffice/callback/{bizType}/{bizId} 文档服务器保存回调（白名单）

## 报价 /quotes

- GET /quotes；GET /quotes/{id}（含 items 与客户）；POST /quotes
- POST /quotes/calculate Aviator 试算 `{items, urgentFactor}`，变量 qty/unit_price/points/complexity/urgent_factor
- POST /quotes/{id}/submit 审批（市场主管）；POST /quotes/{id}/void 作废
- GET /quotes/{id}/export-word POI 生成 Word 报价单下载
- GET/POST /quotes/rules 计价规则维护（表达式编译校验）

## 委托 /entrusts

状态机：DRAFT → REVIEWING →(REVIEW_APPROVE) ACCEPTED → SAMPLING → TESTING → REPORTING → COMPLETED；
REVIEWING 可驳回到 REVIEW_REJECTED；在途可 CANCELLED。

- GET /entrusts；GET /entrusts/{id}（order/items/points/客户/合同）；POST /entrusts
- POST /entrusts/from-quote/{quoteId} 从已审批报价一键带入
- POST /entrusts/{id}/submit-review 提交合同评审（评审员→主管；通过即受理）
- POST /entrusts/{id}/progress 状态推进，body: `{action: START_SAMPLING|START_TESTING|START_REPORT|COMPLETE}`
- POST /entrusts/{id}/cancel；POST /entrusts/{id}/urgent `{urgent,reason}`
- GET /entrusts/{id}/timeline、/items、/points
- POST /entrusts/adjustments 调账（主管+财务两级，通过回写 adjusted_amount）
- GET /entrusts/{id}/adjustments
- POST /entrusts/subcontracts 分包（校验分包方 CMA 资质号，检测主管审批，通过后标记分包项/整单）
- GET /entrusts/{id}/subcontracts

## 审批 /approval

- GET /approval/todo 我的待办（含 bizType/bizId/title/nodeName）
- GET /approval/todo-count
- POST /approval/act `{taskId, approve, comment}`
- GET /approval/timeline/{instanceId}

## 现场采样 /sampling（PC）

状态：计划 DRAFT→ISSUED→CANCELLED；任务 ASSIGNED→SUBMITTED→HANDED；样品 COLLECTED→RECEIVED；交接 PENDING→CONFIRMED/REJECTED。

- GET /sampling/plans（keyword/status/planDateStart/planDateEnd）；GET /sampling/plans/{id}（含 points/items/equipments/tasks）；POST /sampling/plans；POST /sampling/plans/{id}/issue；POST /sampling/plans/{id}/cancel
- GET /sampling/tasks（keyword/status/assigneeId/planId/orderId）；GET /sampling/tasks/{id}（整包含样品照片）；POST /sampling/tasks/assign `{planId,assigneeId,remark}`
- GET /sampling/handovers；GET /sampling/handovers/{id}；POST /sampling/handovers/{id}/confirm `{remark}`（样品接收、任务HANDED、委托自动START_TESTING）；POST /sampling/handovers/{id}/reject `{reason}`
- GET /equipment（category/status）；POST /equipment；GET /equipment/options；GET /equipment/checkouts；POST /equipment/checkouts；POST /equipment/checkouts/{id}/return `{checkResult:OK/DAMAGED/MISSING,returnRemark}`

## 现场采样 /mobile/sampling（移动端）

- GET /mobile/sampling/my-tasks 派给我的任务；GET /mobile/sampling/tasks/{id} 离线整包；POST /mobile/sampling/tasks/{id}/download
- POST /mobile/sampling/samples 提交样品（clientUuid 幂等，photoFileIds 晚绑定 SAMPLE_PHOTO）
- POST /mobile/sampling/tasks/{id}/submit；POST /mobile/sampling/handovers `{taskId,sampleStatus,sigFileId}`；GET /mobile/sampling/handovers/my
- GET /mobile/sampling/samples/{code} 扫码核验；GET /dict/{code} 字典（保存条件等）

## 移动端 /mobile

- GET /mobile/sampling-tasks 采样任务（离线时返回缓存）
- GET /mobile/entrusts/{id}
- POST /mobile/entrusts/{id}/points 现场采样点（BD-09）
- POST /mobile/entrusts/{id}/finish-sampling
- GET /mobile/customers

## 错误码

1001 登录失败 / 1002 账号停用 / 2002 客户已被认领 / 2003 超授信 /
3001 状态不允许 / 3002 任务已处理 / 3003 非当前审批人 / 4001 计价公式错误 / 5001 上传失败
