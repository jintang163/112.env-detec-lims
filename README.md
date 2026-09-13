# LIMS 环境检测实验室信息系统

面向环境检测行业（水/气/声/土壤等）的实验室信息管理系统。本仓库按阶段交付，**阶段一实现「委托与合同管理」**，后续阶段（采样、样品、检测、报告、质控、资源、财务等）在本模块稳定后迭代。

## 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Spring Boot 2.7 · JDK 8 · MyBatis-Plus 3.5 · MySQL 8 · Redis · TDengine 3 · Apache POI · Aviator · MinIO/OSS · JWT + Spring Security · WebSocket ·（流程可切换 Camunda） |
| PC 前端 | Vue 3 + TypeScript + Vite + Ant Design Vue 4 + Pinia + ECharts；OnlyOffice 在线编辑 Word；html2canvas + jsPDF 导出 |
| 移动端 | uni-app(Vue3) + 百度地图 SDK(BD-09) + uCharts + SQLite 离线缓存 |
| 部署 | Docker Compose（仅编排自研应用；MySQL/Redis/Nginx 等用现成镜像） |

## 目录结构

```
.
├── lims-backend/          # Spring Boot 后端（多包单模块，阶段一）
│   └── Dockerfile         # 两阶段构建：maven:3.9-eclipse-temurin-8 → eclipse-temurin:8-jre
├── lims-web/              # PC 前端 Vue3（含 nginx.conf 与 Dockerfile）
├── lims-mobile/           # uni-app 移动端
├── deploy/
│   ├── mysql/init/        # 建表与初始数据（容器首次启动自动执行）
│   ├── docker-compose.yml # 完整依赖栈（mysql/redis/minio/tdengine/onlyoffice，均官方镜像）
│   └── docker-compose.app.yml # 仅编排应用（按需求，中间件用现成的）
└── docs/                  # 架构/接口/数据库说明
```

## 数据库结构

`deploy/mysql/init/01-schema.sql` 共 **32 张表**（实测：`sys_` 系统表 11 张、`biz_` 业务表 21 张）：

- **审批流 4 张**：`biz_approval_flow`（流程/节点定义）、`biz_approval_instance`（审批实例）、`biz_approval_task`（待办任务）、`biz_approval_record`（审批记录）；
- 客户域 5 张、合同域 3 张、委托域 6 张、报价域 3 张；
- 系统域 11 张：用户/角色/权限、字典、文件台账、站内通知、**操作日志 `sys_operation_log`** 等。

## 阶段一功能范围：委托与合同管理

1. **客户管理**：基本信息、资质文件（有效期预警）、A/B/C/D 分级、信用分/授信额度/账期及变更流水、公海客户（认领/退回/分配 + 流转记录）、跟进记录。
2. **合同管理**：合同台账、两级审批（轻量审批流，预留 Camunda 替换点）、履约跟踪（收款计划/实际收款/开票）、合同变更（审批留痕）、合同正文 OnlyOffice 在线编辑。
3. **委托单管理**：创建（项目、检测标准、采样地址与 BD-09 经纬度、期望报告时间）、合同评审（资质/能力/方法/资源）、状态机 `草稿→评审→受理→采样→检测→报告→完成`，支持**加急、调账、分包**，全程状态日志时间线。
4. **报价单管理**：检测项目明细 + **Aviator 公式引擎**按规则自动计价（加急系数、点位、工况系数、折扣）、审批、POI 生成 Word 报价单并支持打印导出。
5. 配套：JWT 登录鉴权、RBAC 权限、站内 WebSocket 实时通知、**操作日志**（AOP 切面自动记录写操作，管理员可在「系统管理 → 操作日志」分页查询）、工作台看板（ECharts）。

## 快速开始

### 1. 启动基础设施

```bash
cd deploy
docker compose up -d mysql redis minio
# MySQL 首次启动会自动执行 deploy/mysql/init 下的 01-schema.sql、02-data.sql
```

中间件也可使用已存在的实例，通过环境变量 `MYSQL_HOST/MYSQL_PORT/MYSQL_USER/MYSQL_PASSWORD`、
`REDIS_HOST/REDIS_PORT/REDIS_PASSWORD` 指定（本地裸机调试可改 `lims-backend/src/main/resources/application-dev.yml`）。

### 2. 后端

要求 JDK 8（已用 Eclipse Temurin 8 验证）：

```bash
cd lims-backend
mvn spring-boot:run            # http://localhost:8080/api
# 打包: mvn -B -DskipTests clean package  → target/lims-backend.jar
```

### 3. PC 前端

```bash
cd lims-web
npm install
npm run dev                    # http://localhost:5173
# 构建(含 vue-tsc 类型检查): npm run build
```

### 4. 移动端

HBuilderX 打开 `lims-mobile` 运行到浏览器/小程序/App；或：

```bash
cd lims-mobile
npm install
npm run dev:h5
```

### 演示账号（密码均为 admin123）

| 账号 | 角色 |
|---|---|
| admin | 系统管理员/总经理（可查看操作日志） |
| manager | 市场主管（审批） |
| sales | 业务员 |
| reviewer | 合同评审员 |
| finance | 财务 |
| sampler | 采样员（移动端） |

## 后续阶段规划（Roadmap）

- 阶段二：采样任务与移动端采样（百度地图打点、条码、样品交接）、TDengine 环境在线监测时序数据接入
- 阶段三：样品管理、检测任务/原始记录、设备/试剂/标准物质、质控（空白/平行/加标）
- 阶段四：报告编制与三级审核、OnlyOffice 报告模板、签章、归档
- 阶段五：财务结算、客户门户、领导驾驶舱、Camunda 流程接入
