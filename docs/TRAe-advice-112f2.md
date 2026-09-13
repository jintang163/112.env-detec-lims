# 智能体任务审查报告

## 1. 审查范围与材料

- 原始需求：轨迹文件 `题112f2-轨迹-20260913-192545/4bc35a3d-792c-4ed5-bb50-a95b9be7b3fe.jsonl` 首条用户消息，要求现场采样管理：计划制定、任务分配与移动端接收、离线现场采样（GPS/照片/温度/pH/二维码/保存条件/质控）、样品交接、设备领用归还。
- 过程材料：同一 JSONL 及 `subagents/` 记录。
- 产物：`112.env-detec-lims` 当前工作树；技能流水线证据见 `audit-evidence-112f2.json`。
- 本报告只记录有文件或轨迹证据支持的结论；未把构建环境缺失作为主要产物问题。

## 2. 目录与结构（含证据）

结构覆盖后端、Web、移动端、SQL 初始化和 API 文档。新增后端 `lims-backend/src/main/java/com/lims/module/sampling/`，移动端 `lims-mobile/src/pages/sampling/`，Web 端 `lims-web/src/views/sampling/` 与 `equipment/`，并修改 `deploy/mysql/init/01-schema.sql`、`02-data.sql`。技能流水线已成功生成证据文件。

## 3. 过程问题（含证据）

1. **权限边界审查遗漏（P0）**。轨迹最后总结称“登录态即可访问，不做方法级权限限制”，并声称已“确认所有相关后端模块无问题”；但过程没有针对移动端接口逐项验证“当前用户是否为任务采样员”。这直接遗漏了任务数据隔离这一核心检查项。
2. **验证声明超出证据（P1）**。轨迹总结称“全部完成”“移动端 build:h5 通过”，但同一总结明确写“本机无 JDK/Maven/MySQL/Docker，后端未能本地编译”，因此“全部完成”不能覆盖后端可运行性；过程只实际展示了 Web 构建成功输出，未展示后端编译或端到端验证。
3. **需求-验证映射不完整（P1）**。原始需求包含移动端接收任务、离线同步、交接和设备归还；过程虽创建对应文件，但没有给出逐项验收结果（例如交接状态迁移、设备库存回补、未授权用户访问）。

## 4. 产物问题（含证据）

1. **移动端接口缺少任务归属校验（P0）**。`lims-backend/src/main/java/com/lims/module/sampling/controller/MobileSamplingController.java` 的 `taskDetail(@PathVariable Long id)` 直接调用 `taskService.detail(id)`，没有比较 `task.assigneeId` 与当前用户；`myTasks` 虽按当前用户过滤，但详情接口可被改 ID 直接读取任意任务、点位、设备和样品照片。
2. **样品提交缺少当前用户校验（P0）**。`lims-backend/src/main/java/com/lims/module/sampling/service/FieldSampleService.java` 的 `submit` 只检查任务存在及状态，未校验 `SecurityUtils.currentUserId()` 是否等于 `task.assigneeId`。任意已登录用户可向他人任务写入样品，且代码把 `task.getAssigneeId/Name` 写入样品，造成责任人伪造。
3. **离线下载确认接口同样未校验归属（P1）**。`MobileSamplingController.downloaded` 调用 `fieldSampleService.markDownloaded(id)`，而 `markDownloaded` 只按 ID 更新下载时间；越权用户可修改其他任务的下载审计字段。
4. **证据不足的功能不能视为已验收（P1）**。轨迹仅展示 Web 构建成功；后端新包包含 44 个 Java 文件，但没有成功编译输出。报告不据此断言编译必然失败，只标记为未验证。

## 5. 总结与产物/过程一致性

总结中列出的页面、API、SQL 表和离线队列在工作树中均能找到对应文件，Web 构建成功也有轨迹输出支持。另一方面，总结的“全部完成/无问题”与实际权限缺口不一致；“后端未能本地编译”与“已完成”的表述存在范围冲突，应改为“代码已生成，后端未完成编译验收”。

## 6. 修复建议（P0/P1/P2）

### P0

- 在移动端任务详情、下载、提交样品、提交任务、发起交接及扫码查询入口统一执行当前用户与任务/样品归属校验；拒绝越权并补充控制器测试。
- 在 `FieldSampleService.submit` 事务内校验 `SecurityUtils.currentUserId()` 与 `task.assigneeId`，不要仅依赖客户端传入的 taskId/clientUuid。

### P1

- 将后端编译、数据库初始化和关键状态流转列为必验收项；总结中区分“已实现”和“已验证”。
- 为移动端 API 增加未授权访问、重复离线提交、交接确认/拒收、设备归还库存回补测试。

### P2

- 在 `docs/API.md` 明确每个移动端接口的角色与数据范围，并把审查报告中的需求-验证矩阵纳入交付记录。

```text
--begin_output--
产物不满意：1、移动端任务详情和样品提交没有校验任务归属，登录用户可读取或写入别人的任务数据。2、后端代码没有完成编译验收，现场采样全流程仍缺少可运行证据。
过程不满意：过程把“全部完成”写得过满，未逐项审查移动端任务归属，导致越权读取和写入问题未被发现。
修复问题：
1、给移动端详情、下载、样品提交、任务提交和交接接口补上当前用户与任务归属校验。
2、在样品提交服务中校验当前用户等于任务采样员，补重复提交和越权访问测试。
3、完成后端编译、数据库初始化和关键状态流转验收，并据实更新交付总结。
--end_output--
```
