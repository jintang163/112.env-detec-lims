# 架构说明

## 模块划分（lims-backend，包名 com.lims）

```
common
├── core        Result/PageResult/异常/BaseEntity/全局异常处理
├── config      MyBatis-Plus(分页)、Redis、CORS
├── security    JWT、Spring Security、审计字段自动填充、当前用户工具
├── storage     FileStorage 抽象: local(默认)/minio 双实现 + 文件台账 + OnlyOffice字节回写
├── ws          WebSocket 连接管理 + NotifyService(落库+异步推送)
├── formula     Aviator 封装(表达式缓存/编译校验/异常归一化)
├── word        Apache POI: {{占位符}} 替换 + {{item.xxx}} 明细行扩展 + docx文本解析
├── log         OperationLogAspect: AOP 环绕写接口自动记录操作日志(sys_operation_log)
└── util        单号生成(Redis 年序列: WT-/HT/BJ/KH)
module
├── system      用户/角色/权限/字典/通知/文件/操作日志查询
├── approval    通用顺序审批流(定义/实例/任务/记录 4 张表 + 回调注册表)
├── customer    客户/公海/资质/信用/跟进
├── contract    合同台账/履约收付款/变更/OnlyOffice
├── quote       报价/明细/Aviator规则计价/POI Word 导出
├── entrust     委托单/检测项/采样点/状态机/评审/调账/分包
├── dashboard   看板聚合
└── mobile      移动端专用接口
```

## 关键设计

1. **轻量审批流**：`biz_approval_flow` 按业务类型配置顺序节点（角色），业务模块实现 `ApprovalCallback`
   接收通过/驳回事件。后续切换 Camunda 时仅替换 ApprovalService 与回调适配，业务代码不动。
2. **委托状态机**：`EntrustStatus.ACTIONS` 统一前置状态校验，所有流转写 `biz_entrust_status_log` 时间线。
3. **报价计价**：规则关键字匹配 + priority 排序，默认兜底 `qty * unit_price`；表达式与结果快照到明细 `formula` 字段。
4. **权限**：JWT 无状态；登录后角色/权限写 Redis（`lims:auth:{uid}`），改权限删 key 即时生效；
   接口用 `@PreAuthorize("hasAuthority('PERM_xxx')")`，前端按 perm 码控制按钮。
5. **通知**：审批派单/审批结果通过 `NotifyService` 异步落库并 WebSocket 推送，前端断线 10s 重连。
6. **文件存储**：默认本地磁盘（免 MinIO 即可演示），配置 `LIMS_STORAGE_TYPE=minio` 切换；
   合同正文通过 OnlyOffice 在线编辑，status=2/6 回调把新版本字节写回存储。
7. **坐标**：统一百度 BD-09；PC/移动端均提供 WGS84→GCJ02→BD09 转换；App 建议直接接百度定位 SDK。
8. **移动离线**：App 用 plus.sqlite，H5/小程序降级 uni.storage；采样点先离线落库，联网一键同步。
9. **TDengine**：驱动已引入，阶段二接入在线监测时序数据（多数据源/REST 写入），本阶段不启用连接。
10. **操作日志**：`common/log/OperationLogAspect` 以 `@within(RestController)` 环绕所有写接口（POST/PUT/PATCH/DELETE，GET 不记），
    按 HTTP 方法+路径映射模块/动作，记录操作人、参数（password 等敏感字段掩码、超长截断）、IP、耗时，日志写入失败不影响业务；
    查询接口 `GET /system/operation-logs` 仅授予 `PERM_system:operation-log`（管理员）。
