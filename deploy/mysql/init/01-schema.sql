-- =====================================================================
-- LIMS 环境检测实验室信息系统  阶段一：委托与合同管理
-- MySQL 8.x  字符集 utf8mb4
-- 命名：sys_ 系统/权限  biz_ 业务
-- =====================================================================
CREATE DATABASE IF NOT EXISTS `lims` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `lims`;

-- =====================================================================
-- 一、系统与权限
-- =====================================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id              BIGINT       NOT NULL PRIMARY KEY COMMENT '用户ID(雪花)',
  username        VARCHAR(64)  NOT NULL COMMENT '登录名',
  password        VARCHAR(128) NOT NULL COMMENT 'BCrypt密文',
  real_name       VARCHAR(64)  NOT NULL COMMENT '姓名',
  dept_id         BIGINT       NULL COMMENT '部门ID',
  phone           VARCHAR(20)  NULL,
  email           VARCHAR(128) NULL,
  avatar          VARCHAR(255) NULL,
  status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  last_login_at   DATETIME     NULL,
  remark          VARCHAR(255) NULL,
  create_by       VARCHAR(64)  NULL,
  update_by       VARCHAR(64)  NULL,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_username (username)
) COMMENT '系统用户';

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  id           BIGINT      NOT NULL PRIMARY KEY,
  role_code    VARCHAR(64) NOT NULL COMMENT '角色编码',
  role_name    VARCHAR(64) NOT NULL,
  data_scope   TINYINT     NOT NULL DEFAULT 1 COMMENT '1全部 2本人 3部门',
  status       TINYINT     NOT NULL DEFAULT 1,
  remark       VARCHAR(255) NULL,
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT    NOT NULL DEFAULT 0,
  UNIQUE KEY uk_role_code (role_code)
) COMMENT '角色';

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
  id      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_role (role_id)
) COMMENT '用户角色';

DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
  id        BIGINT       NOT NULL PRIMARY KEY,
  parent_id BIGINT       NOT NULL DEFAULT 0,
  perm_code VARCHAR(128) NULL COMMENT '权限标识 如 customer:add',
  perm_name VARCHAR(64)  NOT NULL,
  perm_type TINYINT      NOT NULL COMMENT '1菜单 2按钮',
  path      VARCHAR(128) NULL COMMENT '前端路由',
  icon      VARCHAR(64)  NULL,
  sort_no   INT          NOT NULL DEFAULT 0
) COMMENT '菜单/权限';

DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
  id            BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  role_id       BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  UNIQUE KEY uk_role_perm (role_id, permission_id)
) COMMENT '角色权限';

DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
  id        BIGINT      NOT NULL PRIMARY KEY,
  parent_id BIGINT      NOT NULL DEFAULT 0,
  dept_name VARCHAR(64) NOT NULL,
  leader    VARCHAR(64) NULL,
  phone     VARCHAR(20) NULL,
  sort_no   INT         NOT NULL DEFAULT 0,
  create_time DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '部门';

DROP TABLE IF EXISTS sys_dict_type;
CREATE TABLE sys_dict_type (
  id          BIGINT      NOT NULL PRIMARY KEY,
  dict_code   VARCHAR(64) NOT NULL COMMENT '字典编码',
  dict_name   VARCHAR(64) NOT NULL,
  remark      VARCHAR(255) NULL,
  UNIQUE KEY uk_dict_code (dict_code)
) COMMENT '字典类型';

DROP TABLE IF EXISTS sys_dict_data;
CREATE TABLE sys_dict_data (
  id        BIGINT      NOT NULL PRIMARY KEY,
  dict_code VARCHAR(64) NOT NULL,
  item_label VARCHAR(64) NOT NULL,
  item_value VARCHAR(64) NOT NULL,
  sort_no   INT NOT NULL DEFAULT 0,
  css_class VARCHAR(64) NULL COMMENT '标签颜色等',
  status    TINYINT NOT NULL DEFAULT 1,
  KEY idx_dict (dict_code)
) COMMENT '字典项';

DROP TABLE IF EXISTS sys_notification;
CREATE TABLE sys_notification (
  id          BIGINT       NOT NULL PRIMARY KEY,
  user_id     BIGINT       NOT NULL COMMENT '接收人',
  title       VARCHAR(128) NOT NULL,
  content     VARCHAR(512) NULL,
  biz_type    VARCHAR(32)  NULL COMMENT 'CONTRACT/QUOTE/ENTRUST/APPROVAL',
  biz_id      BIGINT       NULL,
  is_read     TINYINT      NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_read (user_id, is_read)
) COMMENT '站内通知(WebSocket推送)';

DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
  id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT       NULL,
  username    VARCHAR(64)  NULL,
  module      VARCHAR(64)  NULL,
  action      VARCHAR(128) NULL,
  method      VARCHAR(255) NULL,
  params      TEXT         NULL,
  ip          VARCHAR(64)  NULL,
  cost_ms     INT          NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '操作日志';

DROP TABLE IF EXISTS sys_file;
CREATE TABLE sys_file (
  id            BIGINT       NOT NULL PRIMARY KEY,
  biz_type      VARCHAR(32)  NULL COMMENT '合同/资质/报价...',
  biz_id        BIGINT       NULL,
  original_name VARCHAR(255) NOT NULL,
  object_name   VARCHAR(255) NOT NULL COMMENT 'MinIO object / 本地相对路径',
  url           VARCHAR(512) NOT NULL,
  content_type  VARCHAR(128) NULL,
  file_size     BIGINT       NULL,
  storage_type  VARCHAR(16)  NOT NULL DEFAULT 'local' COMMENT 'local/minio/oss',
  uploader_id   BIGINT       NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_biz (biz_type, biz_id)
) COMMENT '文件台账';

-- =====================================================================
-- 二、客户管理
-- =====================================================================
DROP TABLE IF EXISTS biz_customer;
CREATE TABLE biz_customer (
  id              BIGINT       NOT NULL PRIMARY KEY,
  code            VARCHAR(32)  NOT NULL COMMENT '客户编号',
  name            VARCHAR(128) NOT NULL COMMENT '客户全称',
  short_name      VARCHAR(64)  NULL,
  customer_type   TINYINT      NOT NULL DEFAULT 10 COMMENT '10企业 20政府事业 30个人',
  industry        VARCHAR(64)  NULL COMMENT '所属行业',
  contact_person  VARCHAR(64)  NULL,
  contact_phone   VARCHAR(32)  NULL,
  email           VARCHAR(128) NULL,
  province        VARCHAR(64)  NULL,
  city            VARCHAR(64)  NULL,
  district        VARCHAR(64)  NULL,
  address         VARCHAR(255) NULL COMMENT '详细地址',
  bank_name       VARCHAR(128) NULL,
  bank_account    VARCHAR(64)  NULL,
  tax_no          VARCHAR(64)  NULL COMMENT '统一社会信用代码/税号',
  customer_level  VARCHAR(8)   NOT NULL DEFAULT 'C' COMMENT 'A/B/C/D 分级',
  credit_score    INT          NOT NULL DEFAULT 60 COMMENT '信用分0-100',
  credit_limit    DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '授信额度',
  credit_period   INT          NOT NULL DEFAULT 0 COMMENT '账期(天)',
  source          VARCHAR(64)  NULL COMMENT '客户来源',
  owner_user_id   BIGINT       NULL COMMENT '归属业务员; NULL=公海',
  pool_status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1公海 2已认领 3已冻结',
  follow_count    INT          NOT NULL DEFAULT 0,
  last_follow_at  DATETIME     NULL,
  status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0停用(黑名单)',
  remark          VARCHAR(512) NULL,
  create_by       VARCHAR(64)  NULL,
  update_by       VARCHAR(64)  NULL,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_code (code),
  KEY idx_name (name),
  KEY idx_owner (owner_user_id),
  KEY idx_pool (pool_status)
) COMMENT '客户';

DROP TABLE IF EXISTS biz_customer_qualification;
CREATE TABLE biz_customer_qualification (
  id           BIGINT      NOT NULL PRIMARY KEY,
  customer_id  BIGINT      NOT NULL,
  qual_type    VARCHAR(64) NOT NULL COMMENT '营业执照/行业资质/授权书...',
  cert_no      VARCHAR(128) NULL,
  file_id      BIGINT      NULL,
  file_name    VARCHAR(255) NULL,
  file_url     VARCHAR(512) NULL,
  valid_from   DATE        NULL,
  valid_to     DATE        NULL COMMENT '有效期至',
  valid_status TINYINT     NOT NULL DEFAULT 1 COMMENT '1有效 2即将过期 3已过期',
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_customer (customer_id)
) COMMENT '客户资质文件';

DROP TABLE IF EXISTS biz_customer_credit_log;
CREATE TABLE biz_customer_credit_log (
  id            BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  customer_id   BIGINT       NOT NULL,
  change_type   TINYINT      NOT NULL COMMENT '1信用分 2授信额度 3账期 4冻结/解冻',
  before_value  VARCHAR(64)  NULL,
  after_value   VARCHAR(64)  NULL,
  reason        VARCHAR(255) NOT NULL,
  operator_id   BIGINT       NULL,
  operator_name VARCHAR(64)  NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_customer (customer_id)
) COMMENT '客户信用变更记录';

DROP TABLE IF EXISTS biz_customer_pool_log;
CREATE TABLE biz_customer_pool_log (
  id            BIGINT      NOT NULL PRIMARY KEY AUTO_INCREMENT,
  customer_id   BIGINT      NOT NULL,
  action        VARCHAR(16) NOT NULL COMMENT 'CLAIM认领 RELEASE退回 TRANSFER分配/转交',
  from_user_id  BIGINT      NULL,
  to_user_id    BIGINT      NULL,
  operator_id   BIGINT      NULL,
  remark        VARCHAR(255) NULL,
  create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_customer (customer_id)
) COMMENT '公海流转记录';

DROP TABLE IF EXISTS biz_customer_follow;
CREATE TABLE biz_customer_follow (
  id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT       NOT NULL,
  content     VARCHAR(1024) NOT NULL COMMENT '跟进内容',
  follow_type VARCHAR(32)  NULL COMMENT '电话/拜访/微信/其他',
  next_time   DATETIME     NULL COMMENT '下次联系时间',
  operator_id BIGINT       NULL,
  operator_name VARCHAR(64) NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_customer (customer_id)
) COMMENT '客户跟进记录';

-- =====================================================================
-- 三、合同管理
-- =====================================================================
DROP TABLE IF EXISTS biz_contract;
CREATE TABLE biz_contract (
  id                BIGINT        NOT NULL PRIMARY KEY,
  code              VARCHAR(32)   NOT NULL COMMENT '合同编号',
  name              VARCHAR(200)  NOT NULL COMMENT '合同名称',
  customer_id       BIGINT        NOT NULL,
  quote_id          BIGINT        NULL COMMENT '来源报价单',
  amount            DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '合同金额(含税)',
  tax_rate          DECIMAL(5,2)  NULL COMMENT '税率%',
  sign_date         DATE          NULL,
  effective_date    DATE          NULL,
  expiry_date       DATE          NULL,
  payment_method    VARCHAR(64)   NULL COMMENT '付款方式',
  payment_terms     VARCHAR(512)  NULL COMMENT '付款条款',
  our_party         VARCHAR(128)  NULL COMMENT '甲方(我方)',
  counter_party     VARCHAR(128)  NULL COMMENT '乙方',
  status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                    COMMENT 'DRAFT草稿/APPROVING审批中/APPROVED已审批/EXECUTING履约中/COMPLETED已完成/CHANGED已变更/TERMINATED已终止',
  received_amount   DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已收款',
  invoiced_amount   DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '已开票',
  file_id           BIGINT        NULL,
  file_url          VARCHAR(512)  NULL COMMENT '合同正文(Word/PDF, OnlyOffice编辑)',
  approval_id       BIGINT        NULL,
  approved_at       DATETIME      NULL,
  remark            VARCHAR(512)  NULL,
  create_by         VARCHAR(64)   NULL,
  update_by         VARCHAR(64)   NULL,
  create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted           TINYINT       NOT NULL DEFAULT 0,
  UNIQUE KEY uk_code (code),
  KEY idx_customer (customer_id),
  KEY idx_status (status)
) COMMENT '合同台账';

DROP TABLE IF EXISTS biz_contract_change;
CREATE TABLE biz_contract_change (
  id             BIGINT       NOT NULL PRIMARY KEY,
  contract_id    BIGINT       NOT NULL,
  change_no      VARCHAR(32)  NOT NULL COMMENT '变更单号',
  change_type    VARCHAR(32)  NOT NULL COMMENT 'AMOUNT金额/PERIOD周期/TERMS条款/OTHER',
  before_content VARCHAR(1024) NULL,
  after_content  VARCHAR(1024) NOT NULL,
  reason         VARCHAR(512) NOT NULL,
  status         VARCHAR(20)  NOT NULL DEFAULT 'APPROVING' COMMENT 'APPROVING/APPROVED/REJECTED',
  approval_id    BIGINT       NULL,
  create_by      VARCHAR(64)  NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_contract (contract_id)
) COMMENT '合同变更记录';

DROP TABLE IF EXISTS biz_contract_payment;
CREATE TABLE biz_contract_payment (
  id           BIGINT        NOT NULL PRIMARY KEY AUTO_INCREMENT,
  contract_id  BIGINT        NOT NULL,
  pay_type     TINYINT       NOT NULL COMMENT '1收款计划 2实际收款 3开票记录',
  plan_date    DATE          NULL,
  occur_date   DATE          NULL,
  amount       DECIMAL(14,2) NOT NULL,
  invoice_no   VARCHAR(64)   NULL COMMENT '发票号',
  remark       VARCHAR(255)  NULL,
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_contract (contract_id)
) COMMENT '合同收款/开票(履约跟踪)';

-- =====================================================================
-- 四、报价管理
-- =====================================================================
DROP TABLE IF EXISTS biz_quote;
CREATE TABLE biz_quote (
  id             BIGINT        NOT NULL PRIMARY KEY,
  code           VARCHAR(32)   NOT NULL,
  title          VARCHAR(200)  NOT NULL,
  customer_id    BIGINT        NOT NULL,
  pricing_mode   VARCHAR(16)   NOT NULL DEFAULT 'RULE' COMMENT 'RULE公式规则/MANUAL手工',
  total_amount   DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '明细合计',
  discount_amount DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '优惠/调减',
  final_amount   DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '报价总额',
  urgent_factor  DECIMAL(5,2)  NOT NULL DEFAULT 1.00 COMMENT '加急系数',
  valid_until    DATE          NULL COMMENT '报价有效期',
  status         VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                 COMMENT 'DRAFT/APPROVING/APPROVED/REJECTED/VOID',
  approval_id    BIGINT        NULL,
  approved_at    DATETIME      NULL,
  file_url       VARCHAR(512)  NULL COMMENT '导出的Word报价单',
  remark         VARCHAR(512)  NULL,
  create_by      VARCHAR(64)   NULL,
  update_by      VARCHAR(64)   NULL,
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT       NOT NULL DEFAULT 0,
  UNIQUE KEY uk_code (code),
  KEY idx_customer (customer_id)
) COMMENT '报价单';

DROP TABLE IF EXISTS biz_quote_item;
CREATE TABLE biz_quote_item (
  id          BIGINT        NOT NULL PRIMARY KEY AUTO_INCREMENT,
  quote_id    BIGINT        NOT NULL,
  item_name   VARCHAR(200)  NOT NULL COMMENT '检测项目/参数',
  standard_code VARCHAR(64) NULL COMMENT '检测标准号',
  spec        VARCHAR(128)  NULL COMMENT '规格/点位说明',
  unit        VARCHAR(32)   NULL COMMENT '计量单位',
  qty         DECIMAL(10,2) NOT NULL DEFAULT 1 COMMENT '数量/点次',
  unit_price  DECIMAL(12,2) NOT NULL DEFAULT 0,
  amount      DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT 'Aviator公式计算结果',
  formula     VARCHAR(255)  NULL COMMENT '命中的计价表达式快照',
  remark      VARCHAR(255)  NULL,
  sort_no     INT           NOT NULL DEFAULT 0,
  KEY idx_quote (quote_id)
) COMMENT '报价明细';

DROP TABLE IF EXISTS biz_quote_pricing_rule;
CREATE TABLE biz_quote_pricing_rule (
  id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  rule_name   VARCHAR(128) NOT NULL,
  keyword     VARCHAR(128) NULL COMMENT '项目名称包含的关键字(命中即用)',
  expression  VARCHAR(512) NOT NULL COMMENT 'Aviator表达式, 变量: qty/unit_price/points/complexity/urgent_factor',
  priority    INT          NOT NULL DEFAULT 100 COMMENT '数字小优先',
  status      TINYINT      NOT NULL DEFAULT 1,
  remark      VARCHAR(255) NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '报价计价规则(Aviator公式)';

-- =====================================================================
-- 五、委托单管理
-- =====================================================================
DROP TABLE IF EXISTS biz_entrust_order;
CREATE TABLE biz_entrust_order (
  id                 BIGINT        NOT NULL PRIMARY KEY,
  code               VARCHAR(32)   NOT NULL COMMENT '委托单号 WT-yyyy-xxxx',
  title              VARCHAR(200)  NOT NULL COMMENT '项目名称',
  customer_id        BIGINT        NOT NULL,
  contract_id        BIGINT        NULL COMMENT '关联合同(个人散客可空)',
  quote_id           BIGINT        NULL,
  entrust_type       VARCHAR(16)   NOT NULL DEFAULT 'ENTRUST' COMMENT 'ENTRUST委托/SUPERVISION监督/SPOT抽查',
  urgency            VARCHAR(8)    NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL普通/URGENT加急',
  status             VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                     COMMENT 'DRAFT草稿/REVIEWING评审中/REVIEW_REJECTED评审驳回/ACCEPTED已受理/SAMPLING采样中/TESTING检测中/REPORTING报告中/COMPLETED已完成/CANCELLED已取消',
  sample_source      TINYINT       NOT NULL DEFAULT 1 COMMENT '1现场采样 2客户送样',
  contact_person     VARCHAR(64)   NULL COMMENT '现场联系人',
  contact_phone      VARCHAR(32)   NULL,
  province           VARCHAR(64)  NULL,
  city               VARCHAR(64)  NULL,
  district           VARCHAR(64)  NULL,
  sampling_address   VARCHAR(255)  NULL COMMENT '采样地址',
  lng                DECIMAL(12,7) NULL COMMENT '经度(BD-09)',
  lat                DECIMAL(12,7) NULL COMMENT '纬度(BD-09)',
  planned_sampling_time DATETIME   NULL COMMENT '计划采样时间',
  expected_report_date  DATE       NULL COMMENT '期望报告时间',
  total_amount       DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '应收金额(报价)',
  adjusted_amount    DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '调账后金额',
  adjustment_reason  VARCHAR(255)  NULL,
  has_subcontract    TINYINT       NOT NULL DEFAULT 0 COMMENT '是否分包',
  reviewer_id        BIGINT        NULL COMMENT '评审人',
  review_time        DATETIME      NULL,
  review_opinion     VARCHAR(512)  NULL COMMENT '合同评审意见(资质/能力/方法/资源)',
  accepted_at        DATETIME      NULL,
  sampling_at        DATETIME      NULL,
  testing_at         DATETIME      NULL,
  reporting_at       DATETIME      NULL,
  completed_at       DATETIME      NULL,
  cancel_reason      VARCHAR(255)  NULL,
  approval_id        BIGINT        NULL,
  remark             VARCHAR(512)  NULL,
  create_by          VARCHAR(64)   NULL,
  update_by          VARCHAR(64)   NULL,
  create_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted            TINYINT       NOT NULL DEFAULT 0,
  UNIQUE KEY uk_code (code),
  KEY idx_customer (customer_id),
  KEY idx_contract (contract_id),
  KEY idx_status (status),
  KEY idx_urgency (urgency)
) COMMENT '委托单';

DROP TABLE IF EXISTS biz_entrust_item;
CREATE TABLE biz_entrust_item (
  id            BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  order_id      BIGINT       NOT NULL,
  item_name     VARCHAR(200) NOT NULL COMMENT '检测项目/参数',
  standard_code VARCHAR(64)  NULL COMMENT '检测标准号(如 HJ 535-2009)',
  standard_name VARCHAR(255) NULL COMMENT '标准名称',
  sample_name   VARCHAR(128) NULL COMMENT '样品名称',
  sample_qty    INT          NULL COMMENT '样品数量',
  qty           DECIMAL(10,2) NOT NULL DEFAULT 1 COMMENT '点次/频次',
  unit_price    DECIMAL(12,2) NOT NULL DEFAULT 0,
  amount        DECIMAL(14,2) NOT NULL DEFAULT 0,
  is_subcontract TINYINT     NOT NULL DEFAULT 0 COMMENT '该项目是否分包',
  limit_value   VARCHAR(128) NULL COMMENT '限值/判定要求',
  sort_no       INT          NOT NULL DEFAULT 0,
  remark        VARCHAR(255) NULL,
  KEY idx_order (order_id)
) COMMENT '委托检测项';

DROP TABLE IF EXISTS biz_entrust_sampling_point;
CREATE TABLE biz_entrust_sampling_point (
  id        BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  order_id  BIGINT       NOT NULL,
  name      VARCHAR(128) NOT NULL COMMENT '点位名称(如 1#排气筒)',
  lng       DECIMAL(12,7) NOT NULL COMMENT 'BD-09经度',
  lat       DECIMAL(12,7) NOT NULL COMMENT 'BD-09纬度',
  addr_desc VARCHAR(255) NULL,
  sort_no   INT          NOT NULL DEFAULT 0,
  KEY idx_order (order_id)
) COMMENT '采样点位(移动端百度地图采集)';

DROP TABLE IF EXISTS biz_entrust_status_log;
CREATE TABLE biz_entrust_status_log (
  id          BIGINT      NOT NULL PRIMARY KEY AUTO_INCREMENT,
  order_id    BIGINT      NOT NULL,
  from_status VARCHAR(20) NULL,
  to_status   VARCHAR(20) NOT NULL,
  action      VARCHAR(32) NULL COMMENT 'SUBMIT/ACCEPT/START_SAMPLING...',
  operator_id BIGINT      NULL,
  operator_name VARCHAR(64) NULL,
  remark      VARCHAR(255) NULL,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order (order_id)
) COMMENT '委托单状态流转日志';

DROP TABLE IF EXISTS biz_entrust_subcontract;
CREATE TABLE biz_entrust_subcontract (
  id              BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  order_id        BIGINT       NOT NULL,
  item_id         BIGINT       NULL COMMENT '空=整单分包',
  subcontractor   VARCHAR(128) NOT NULL COMMENT '分包方(合作实验室)',
  qual_cert       VARCHAR(128) NULL COMMENT '分包方资质(CMA证书号)',
  qual_file_url   VARCHAR(512) NULL,
  amount          DECIMAL(14,2) NULL,
  reason          VARCHAR(255) NULL COMMENT '分包原因(能力不足/设备占用...)',
  status          VARCHAR(20)  NOT NULL DEFAULT 'APPROVING' COMMENT 'APPROVING/APPROVED/REJECTED',
  approval_id     BIGINT       NULL,
  create_by       VARCHAR(64)  NULL,
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order (order_id)
) COMMENT '分包登记';

DROP TABLE IF EXISTS biz_entrust_adjustment;
CREATE TABLE biz_entrust_adjustment (
  id            BIGINT        NOT NULL PRIMARY KEY AUTO_INCREMENT,
  order_id      BIGINT        NOT NULL,
  adjust_type   TINYINT       NOT NULL COMMENT '1调增 2调减/优惠',
  before_amount DECIMAL(14,2) NOT NULL,
  adjust_amount DECIMAL(14,2) NOT NULL,
  after_amount  DECIMAL(14,2) NOT NULL,
  reason        VARCHAR(255)  NOT NULL,
  status        VARCHAR(20)   NOT NULL DEFAULT 'APPROVING' COMMENT 'APPROVING/APPROVED/REJECTED',
  approval_id   BIGINT       NULL,
  create_by     VARCHAR(64)  NULL,
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order (order_id)
) COMMENT '委托单调账记录';

-- =====================================================================
-- 六、通用审批流（轻量；后期可替换为 Camunda）
-- =====================================================================
DROP TABLE IF EXISTS biz_approval_flow;
CREATE TABLE biz_approval_flow (
  id        BIGINT      NOT NULL PRIMARY KEY AUTO_INCREMENT,
  biz_type  VARCHAR(32) NOT NULL COMMENT 'CONTRACT/QUOTE/ENTRUST_REVIEW/ADJUSTMENT/SUBCONTRACT/CONTRACT_CHANGE',
  node_seq  INT         NOT NULL COMMENT '节点顺序 1..N',
  node_name VARCHAR(64) NOT NULL,
  role_code VARCHAR(64) NOT NULL COMMENT '审批角色',
  UNIQUE KEY uk_type_seq (biz_type, node_seq)
) COMMENT '审批流定义(按业务类型顺序节点)';

DROP TABLE IF EXISTS biz_approval_instance;
CREATE TABLE biz_approval_instance (
  id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  biz_type    VARCHAR(32)  NOT NULL,
  biz_id      BIGINT       NOT NULL,
  title       VARCHAR(200) NOT NULL,
  status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  current_seq INT          NOT NULL DEFAULT 1,
  initiator_id BIGINT      NULL,
  initiator_name VARCHAR(64) NULL,
  started_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME     NULL,
  KEY idx_biz (biz_type, biz_id),
  KEY idx_status (status)
) COMMENT '审批实例';

DROP TABLE IF EXISTS biz_approval_task;
CREATE TABLE biz_approval_task (
  id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  instance_id BIGINT       NOT NULL,
  node_seq    INT          NOT NULL,
  node_name   VARCHAR(64)  NOT NULL,
  role_code   VARCHAR(64)  NOT NULL COMMENT '待办角色(同角色人均可审批)',
  assignee_id BIGINT       NULL COMMENT '实际审批人',
  assignee_name VARCHAR(64) NULL,
  status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  comment     VARCHAR(512) NULL,
  acted_at    DATETIME     NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_instance (instance_id),
  KEY idx_role_status (role_code, status)
) COMMENT '审批任务(待办)';

DROP TABLE IF EXISTS biz_approval_record;
CREATE TABLE biz_approval_record (
  id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  instance_id BIGINT       NOT NULL,
  node_seq    INT          NOT NULL,
  node_name   VARCHAR(64)  NOT NULL,
  approver_id BIGINT       NULL,
  approver_name VARCHAR(64) NULL,
  action      VARCHAR(16)  NOT NULL COMMENT 'APPROVE/REJECT',
  comment     VARCHAR(512) NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_instance (instance_id)
) COMMENT '审批流转记录(时间线)';

-- =====================================================================
-- 七、现场采样管理（采样计划/任务派工/现场样品/样品交接/设备领用）
-- =====================================================================

-- 采样计划
DROP TABLE IF EXISTS biz_sampling_plan;
CREATE TABLE biz_sampling_plan (
  id              BIGINT        NOT NULL PRIMARY KEY COMMENT '计划ID(雪花)',
  code            VARCHAR(32)   NOT NULL COMMENT '计划编号 CYJHyyyy-xxxx',
  order_id        BIGINT        NOT NULL COMMENT '委托单ID',
  title           VARCHAR(200)  NOT NULL COMMENT '计划名称(默认项目名称)',
  customer_id     BIGINT        NULL,
  customer_name   VARCHAR(128)  NULL COMMENT '客户名称快照',
  plan_date       DATE          NOT NULL COMMENT '采样日期',
  start_time      DATETIME      NULL COMMENT '计划开始时间',
  end_time        DATETIME      NULL COMMENT '计划结束时间',
  contact_person  VARCHAR(64)   NULL COMMENT '现场联系人快照',
  contact_phone   VARCHAR(32)   NULL,
  address         VARCHAR(255)  NULL COMMENT '采样地址快照',
  lng             DECIMAL(12,7) NULL COMMENT '经度(BD-09)',
  lat             DECIMAL(12,7) NULL COMMENT '纬度(BD-09)',
  weather         VARCHAR(64)   NULL COMMENT '天气情况',
  status          VARCHAR(16)   NOT NULL DEFAULT 'DRAFT'
                  COMMENT 'DRAFT草稿/ISSUED已下发/CANCELLED已取消',
  remark          VARCHAR(512)  NULL,
  create_by       VARCHAR(64)   NULL,
  update_by       VARCHAR(64)   NULL,
  create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT       NOT NULL DEFAULT 0,
  UNIQUE KEY uk_code (code),
  KEY idx_order (order_id),
  KEY idx_status (status),
  KEY idx_plan_date (plan_date)
) COMMENT '采样计划';

-- 计划点位清单
DROP TABLE IF EXISTS biz_sampling_plan_point;
CREATE TABLE biz_sampling_plan_point (
  id          BIGINT        NOT NULL PRIMARY KEY AUTO_INCREMENT,
  plan_id     BIGINT        NOT NULL,
  point_id    BIGINT        NULL COMMENT '关联委托点位ID(现场新增可空)',
  name        VARCHAR(128)  NOT NULL COMMENT '点位名称(如 1#排气筒)',
  lng         DECIMAL(12,7) NOT NULL COMMENT 'BD-09经度',
  lat         DECIMAL(12,7) NOT NULL COMMENT 'BD-09纬度',
  addr_desc   VARCHAR(255)  NULL,
  sort_no     INT           NOT NULL DEFAULT 0,
  KEY idx_plan (plan_id)
) COMMENT '采样计划点位清单';

-- 计划检测项/样品要求
DROP TABLE IF EXISTS biz_sampling_plan_item;
CREATE TABLE biz_sampling_plan_item (
  id             BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  plan_id        BIGINT       NOT NULL,
  point_id       BIGINT       NULL COMMENT '所属计划点位ID(空表示通用)',
  order_item_id  BIGINT       NULL COMMENT '关联委托检测项ID',
  item_name      VARCHAR(200) NOT NULL COMMENT '检测项目/参数',
  sample_name    VARCHAR(128) NULL COMMENT '样品名称',
  sample_qty     INT          NULL COMMENT '样品数量',
  container      VARCHAR(128) NULL COMMENT '采样容器(如 棕色玻璃瓶/聚乙烯瓶)',
  preservation   VARCHAR(128) NULL COMMENT '保存条件(常温/冷藏/冷冻/避光/密封)',
  qc_required    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否需要质控样',
  sort_no        INT          NOT NULL DEFAULT 0,
  KEY idx_plan (plan_id)
) COMMENT '采样计划检测项/样品要求';

-- 计划携带设备/容器
DROP TABLE IF EXISTS biz_sampling_plan_equipment;
CREATE TABLE biz_sampling_plan_equipment (
  id              BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  plan_id         BIGINT       NOT NULL,
  equipment_id    BIGINT       NOT NULL,
  equipment_name  VARCHAR(128) NOT NULL COMMENT '设备名称快照',
  qty             INT          NOT NULL DEFAULT 1 COMMENT '数量',
  KEY idx_plan (plan_id)
) COMMENT '采样计划携带设备/容器';

-- 采样任务(派工)
DROP TABLE IF EXISTS biz_sampling_task;
CREATE TABLE biz_sampling_task (
  id             BIGINT       NOT NULL PRIMARY KEY COMMENT '任务ID(雪花)',
  code           VARCHAR(32)  NOT NULL COMMENT '任务编号 CYRWyyyy-xxxx(即样品编号前缀)',
  plan_id        BIGINT       NOT NULL,
  order_id       BIGINT       NOT NULL,
  assignee_id    BIGINT       NOT NULL COMMENT '采样员ID',
  assignee_name  VARCHAR(64)  NOT NULL COMMENT '采样员姓名快照',
  assigned_by    VARCHAR(64)  NULL COMMENT '派工人',
  assigned_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  downloaded_at  DATETIME     NULL COMMENT '移动端下载时间',
  started_at     DATETIME     NULL COMMENT '开始采样时间(首次提交样品)',
  submitted_at   DATETIME     NULL COMMENT '采样完成提交时间',
  status         VARCHAR(16)  NOT NULL DEFAULT 'ASSIGNED'
                 COMMENT 'ASSIGNED已分配/SUBMITTED已采样待交接/HANDED已交接/CANCELLED已取消',
  remark         VARCHAR(512) NULL,
  create_by      VARCHAR(64)  NULL,
  update_by      VARCHAR(64)  NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_code (code),
  KEY idx_plan (plan_id),
  KEY idx_order (order_id),
  KEY idx_assignee (assignee_id),
  KEY idx_status (status)
) COMMENT '采样任务分配';

-- 现场样品
DROP TABLE IF EXISTS biz_field_sample;
CREATE TABLE biz_field_sample (
  id               BIGINT        NOT NULL PRIMARY KEY COMMENT '样品ID(雪花)',
  sample_code      VARCHAR(48)   NOT NULL COMMENT '样品编号/二维码内容(任务号-序号)',
  client_uuid      VARCHAR(40)   NOT NULL COMMENT '端上UUID,离线提交幂等键',
  task_id          BIGINT        NOT NULL,
  plan_id          BIGINT        NOT NULL,
  order_id         BIGINT        NOT NULL,
  point_id         BIGINT        NULL COMMENT '计划点位ID',
  point_name       VARCHAR(128)  NULL,
  entrust_item_id  BIGINT        NULL COMMENT '委托检测项ID',
  item_name        VARCHAR(200)  NULL COMMENT '检测项目',
  sample_name      VARCHAR(128)  NULL COMMENT '样品名称',
  sampling_time    DATETIME      NULL COMMENT '采样时间',
  lng              DECIMAL(12,7) NULL COMMENT '实际采样经度(BD-09)',
  lat              DECIMAL(12,7) NULL COMMENT '实际采样纬度(BD-09)',
  addr_desc        VARCHAR(255)  NULL,
  temperature      DECIMAL(5,2)  NULL COMMENT '现场温度(℃)',
  ph               DECIMAL(5,2)  NULL COMMENT '现场pH',
  params_json      TEXT          NULL COMMENT '其他现场参数键值JSON',
  container        VARCHAR(128)  NULL COMMENT '容器',
  storage_condition VARCHAR(128) NULL COMMENT '保存条件',
  is_qc            TINYINT       NOT NULL DEFAULT 0 COMMENT '是否质控样',
  qc_type          VARCHAR(16)   NULL COMMENT 'BLANK全程序空白/PARALLEL平行样/SPIKE加标样',
  status           VARCHAR(16)   NOT NULL DEFAULT 'COLLECTED'
                   COMMENT 'COLLECTED已采集/RECEIVED已接收',
  handover_id      BIGINT        NULL COMMENT '交接单ID',
  sampler_id       BIGINT        NULL,
  sampler_name     VARCHAR(64)   NULL,
  remark           VARCHAR(512)  NULL,
  create_by        VARCHAR(64)   NULL,
  update_by        VARCHAR(64)   NULL,
  create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted          TINYINT       NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sample_code (sample_code),
  UNIQUE KEY uk_client_uuid (client_uuid),
  KEY idx_task (task_id),
  KEY idx_order (order_id),
  KEY idx_handover (handover_id),
  KEY idx_status (status)
) COMMENT '现场样品';

-- 样品交接单
DROP TABLE IF EXISTS biz_sample_handover;
CREATE TABLE biz_sample_handover (
  id               BIGINT       NOT NULL PRIMARY KEY COMMENT '交接单ID(雪花)',
  code             VARCHAR(32)  NOT NULL COMMENT '交接单编号 JJyyyy-xxxx',
  task_id          BIGINT       NOT NULL,
  plan_id          BIGINT       NOT NULL,
  order_id         BIGINT       NOT NULL,
  sample_count     INT          NOT NULL DEFAULT 0 COMMENT '交接样品数量',
  sample_status    VARCHAR(255) NULL COMMENT '样品状态描述(完好/异常说明)',
  handover_by_id   BIGINT       NULL COMMENT '移交人(采样员)',
  handover_by_name VARCHAR(64)  NULL,
  handover_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sig_file_id      BIGINT       NULL COMMENT '采样员签名文件ID(sys_file)',
  receiver_id      BIGINT       NULL COMMENT '接收人(样品管理员)',
  receiver_name    VARCHAR(64)  NULL,
  receive_at       DATETIME     NULL,
  receiver_remark  VARCHAR(512) NULL COMMENT '接收备注/拒收原因',
  status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING'
                   COMMENT 'PENDING待接收/CONFIRMED已接收/REJECTED已拒收',
  create_by        VARCHAR(64)  NULL,
  update_by        VARCHAR(64)  NULL,
  create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted          TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_code (code),
  KEY idx_task (task_id),
  KEY idx_order (order_id),
  KEY idx_status (status)
) COMMENT '样品交接单';

-- 采样设备/容器台账
DROP TABLE IF EXISTS biz_equipment;
CREATE TABLE biz_equipment (
  id             BIGINT       NOT NULL PRIMARY KEY COMMENT '设备ID(雪花)',
  code           VARCHAR(32)  NOT NULL COMMENT '设备编号 SByyyy-xxxx',
  name           VARCHAR(128) NOT NULL COMMENT '设备/容器名称',
  category       VARCHAR(16)  NOT NULL DEFAULT 'DEVICE' COMMENT 'DEVICE设备/CONTAINER容器',
  spec           VARCHAR(128) NULL COMMENT '规格型号',
  unit           VARCHAR(16)  NULL DEFAULT '台' COMMENT '单位',
  qty_total      INT          NOT NULL DEFAULT 1 COMMENT '总数量',
  qty_available  INT          NOT NULL DEFAULT 1 COMMENT '可用数量',
  status         VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL正常/MAINTENANCE维修中/SCRAPPED报废',
  keeper_id      BIGINT       NULL COMMENT '保管人ID',
  keeper_name    VARCHAR(64)  NULL,
  purchase_date  DATE         NULL,
  remark         VARCHAR(512) NULL,
  create_by      VARCHAR(64)  NULL,
  update_by      VARCHAR(64)  NULL,
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT      NOT NULL DEFAULT 0,
  UNIQUE KEY uk_code (code),
  KEY idx_category (category),
  KEY idx_status (status)
) COMMENT '采样设备/容器台账';

-- 设备领用归还记录
DROP TABLE IF EXISTS biz_equipment_checkout;
CREATE TABLE biz_equipment_checkout (
  id                   BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
  equipment_id         BIGINT       NOT NULL,
  equipment_name       VARCHAR(128) NOT NULL COMMENT '设备名称快照',
  plan_id              BIGINT       NULL COMMENT '关联采样计划',
  task_id              BIGINT       NULL COMMENT '关联采样任务',
  qty                  INT          NOT NULL DEFAULT 1,
  checkout_by_id       BIGINT       NULL,
  checkout_by_name     VARCHAR(64)  NULL,
  checkout_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expected_return_time DATETIME     NULL COMMENT '预计归还时间',
  checkout_remark      VARCHAR(255) NULL,
  status               VARCHAR(16)  NOT NULL DEFAULT 'BORROWED' COMMENT 'BORROWED已领用/RETURNED已归还',
  return_time          DATETIME     NULL,
  return_by_id         BIGINT       NULL,
  return_by_name       VARCHAR(64)  NULL,
  check_result         VARCHAR(16)   NULL COMMENT 'OK完好/DAMAGED损坏/MISSING缺失',
  return_remark        VARCHAR(255) NULL,
  create_by            VARCHAR(64)  NULL,
  create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_equipment (equipment_id),
  KEY idx_plan (plan_id),
  KEY idx_status (status)
) COMMENT '采样设备领用归还记录';
