-- =====================================================================
-- 初始数据  初始账号: admin / admin123 (BCrypt)
-- 角色: ROLE_ADMIN 管理员 / ROLE_MANAGER 业务主管 / ROLE_SALES 业务员
--       ROLE_REVIEW 合同评审员 / ROLE_FINANCE 财务 / ROLE_SAMPLER 采样员
-- =====================================================================
USE `lims`;

-- 部门
INSERT INTO sys_dept(id,parent_id,dept_name,leader,sort_no) VALUES
(1,0,'某某环境检测有限公司','张总',1),
(2,1,'市场部','李经理',1),
(3,1,'合同评审组','王工',2),
(4,1,'检测部','赵工',3),
(5,1,'采样部','钱队长',4),
(6,1,'财务部','孙会计',5);

-- 用户 (密码均 admin123)
INSERT INTO sys_user(id,username,password,real_name,dept_id,phone,status) VALUES
(1,'admin','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','系统管理员',1,'13800000001',1),
(2,'manager','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','李经理(市场主管)',2,'13800000002',1),
(3,'sales','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','周业务员',2,'13800000003',1),
(4,'reviewer','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','王评审员',3,'13800000004',1),
(5,'finance','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','孙会计',6,'13800000005',1),
(6,'sampler','$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','钱采样员',5,'13800000006',1);

-- 角色
INSERT INTO sys_role(id,role_code,role_name,data_scope,remark) VALUES
(1,'ROLE_ADMIN','系统管理员',1,'全部数据与权限'),
(2,'ROLE_MANAGER','市场主管',1,'审批/分配/查看全部客户'),
(3,'ROLE_SALES','业务员',2,'本人客户与委托'),
(4,'ROLE_REVIEW','合同评审员',1,'委托单合同评审'),
(5,'ROLE_FINANCE','财务',1,'收款/开票/调账复核'),
(6,'ROLE_SAMPLER','采样员',1,'移动端采样');

INSERT INTO sys_user_role(user_id,role_id) VALUES
(1,1),(2,2),(3,3),(4,4),(5,5),(6,6),(2,3);

-- 菜单/权限
INSERT INTO sys_permission(id,parent_id,perm_code,perm_name,perm_type,path,icon,sort_no) VALUES
(100,0,'dashboard','工作台',1,'/dashboard','DashboardOutlined',1),
(200,0,'customer','客户管理',1,'/customer','TeamOutlined',10),
(201,200,'customer:list','客户查询',2,NULL,NULL,1),
(202,200,'customer:add','客户新增',2,NULL,NULL,2),
(203,200,'customer:edit','客户编辑',2,NULL,NULL,3),
(204,200,'customer:pool','公海操作',2,NULL,NULL,4),
(205,200,'customer:credit','信用管理',2,NULL,NULL,5),
(300,0,'contract','合同管理',1,'/contract','FileProtectOutlined',20),
(301,300,'contract:list','合同查询',2,NULL,NULL,1),
(302,300,'contract:add','合同新增',2,NULL,NULL,2),
(303,300,'contract:approve','合同审批',2,NULL,NULL,3),
(304,300,'contract:change','合同变更',2,NULL,NULL,4),
(400,0,'entrust','委托管理',1,'/entrust','AuditOutlined',30),
(401,400,'entrust:list','委托查询',2,NULL,NULL,1),
(402,400,'entrust:add','委托创建',2,NULL,NULL,2),
(403,400,'entrust:review','合同评审',2,NULL,NULL,3),
(404,400,'entrust:progress','状态推进',2,NULL,NULL,4),
(405,400,'entrust:adjust','调账',2,NULL,NULL,5),
(406,400,'entrust:subcontract','分包',2,NULL,NULL,6),
(500,0,'quote','报价管理',1,'/quote','CalculatorOutlined',40),
(501,500,'quote:list','报价查询',2,NULL,NULL,1),
(502,500,'quote:add','报价编制',2,NULL,NULL,2),
(503,500,'quote:approve','报价审批',2,NULL,NULL,3),
(504,500,'quote:export','报价导出',2,NULL,NULL,4),
(600,0,'approval','审批中心',1,'/approval','CheckSquareOutlined',50),
(601,600,'approval:todo','待办审批',2,NULL,NULL,1),
(900,0,'system','系统管理',1,'/system','SettingOutlined',90),
(901,900,'system:operation-log','操作日志',1,'/system/operation-log','FileSearchOutlined',1);

-- admin 拥有全部权限；其他角色给业务权限
INSERT INTO sys_role_permission(role_id,permission_id)
SELECT 1,id FROM sys_permission;
INSERT INTO sys_role_permission(role_id,permission_id) VALUES
(2,100),(2,200),(2,201),(2,202),(2,203),(2,204),(2,205),
(2,300),(2,301),(2,302),(2,303),(2,304),
(2,400),(2,401),(2,402),(2,403),(2,404),(2,405),(2,406),
(2,500),(2,501),(2,502),(2,503),(2,504),(2,600),(2,601),
(3,100),(3,200),(3,201),(3,202),(3,203),(3,204),
(3,300),(3,301),(3,302),
(3,400),(3,401),(3,402),(3,404),
(3,500),(3,501),(3,502),(3,504),(3,600),(3,601),
(4,100),(4,400),(4,401),(4,403),(4,600),(4,601),
(5,100),(5,300),(5,301),(5,400),(5,401),(5,405),(5,600),(5,601),
(6,100),(6,400),(6,401),(6,404);

-- 字典
INSERT INTO sys_dict_type(id,dict_code,dict_name) VALUES
(1,'customer_level','客户分级'),(2,'customer_type','客户类型'),(3,'entrust_status','委托状态'),
(4,'urgency','加急标识'),(5,'contract_status','合同状态'),(6,'quote_status','报价状态');
INSERT INTO sys_dict_data(id,dict_code,item_label,item_value,sort_no,css_class) VALUES
(101,'customer_level','A级','A',1,'red'),(102,'customer_level','B级','B',2,'orange'),
(103,'customer_level','C级','C',3,'blue'),(104,'customer_level','D级','D',4,'default'),
(111,'customer_type','企业','10',1,'blue'),(112,'customer_type','政府事业单位','20',2,'green'),(113,'customer_type','个人','30',3,'default'),
(201,'entrust_status','草稿','DRAFT',1,'default'),(202,'entrust_status','评审中','REVIEWING',2,'processing'),
(203,'entrust_status','评审驳回','REVIEW_REJECTED',3,'error'),(204,'entrust_status','已受理','ACCEPTED',4,'cyan'),
(205,'entrust_status','采样中','SAMPLING',5,'blue'),(206,'entrust_status','检测中','TESTING',6,'geekblue'),
(207,'entrust_status','报告中','REPORTING',7,'purple'),(208,'entrust_status','已完成','COMPLETED',8,'green'),
(209,'entrust_status','已取消','CANCELLED',9,'default'),
(301,'urgency','普通','NORMAL',1,'default'),(302,'urgency','加急','URGENT',2,'red'),
(401,'contract_status','草稿','DRAFT',1,'default'),(402,'contract_status','审批中','APPROVING',2,'processing'),
(403,'contract_status','已审批','APPROVED',3,'cyan'),(404,'contract_status','履约中','EXECUTING',4,'blue'),
(405,'contract_status','已完成','COMPLETED',5,'green'),(406,'contract_status','已变更','CHANGED',6,'orange'),
(407,'contract_status','已终止','TERMINATED',9,'error'),
(501,'quote_status','草稿','DRAFT',1,'default'),(502,'quote_status','审批中','APPROVING',2,'processing'),
(503,'quote_status','已审批','APPROVED',3,'green'),(504,'quote_status','已驳回','REJECTED',4,'error'),(505,'quote_status','已作废','VOID',9,'default');

-- 审批流定义
INSERT INTO biz_approval_flow(biz_type,node_seq,node_name,role_code) VALUES
('CONTRACT',1,'市场主管审批','ROLE_MANAGER'),
('CONTRACT',2,'总经理审批','ROLE_ADMIN'),
('QUOTE',1,'市场主管审批','ROLE_MANAGER'),
('ENTRUST_REVIEW',1,'合同评审(资质/能力/方法)','ROLE_REVIEW'),
('ENTRUST_REVIEW',2,'市场主管确认','ROLE_MANAGER'),
('ADJUSTMENT',1,'市场主管审批','ROLE_MANAGER'),
('ADJUSTMENT',2,'财务复核','ROLE_FINANCE'),
('SUBCONTRACT',1,'检测部主管审批','ROLE_REVIEW'),
('CONTRACT_CHANGE',1,'市场主管审批','ROLE_MANAGER'),
('CONTRACT_CHANGE',2,'总经理审批','ROLE_ADMIN');

-- 计价规则 (Aviator 表达式；变量 qty, unit_price, points, complexity, urgent_factor)
INSERT INTO biz_quote_pricing_rule(rule_name,keyword,expression,priority,remark) VALUES
('默认线性计价',NULL,'qty * unit_price',999,'数量×单价'),
('加急附加费','加急','qty * unit_price * urgent_factor',10,'命中加急关键字时按加急系数'),
('有组织废气点位','废气','points * unit_price * complexity',20,'点位数×单价×工况系数'),
('水质套餐折扣','水质','qty * unit_price * 0.95',30,'水质批量95折');

-- 示例客户（含一个公海客户）
INSERT INTO biz_customer(id,code,name,short_name,customer_type,industry,contact_person,contact_phone,
 province,city,district,address,customer_level,credit_score,credit_limit,credit_period,source,owner_user_id,pool_status,status,create_by)
VALUES
(1001,'KH20260001','华东制药股份有限公司','华东制药',10,'制药','陈工','13900001111','江苏省','南京市','江宁区','科学园药谷路8号','A',90,500000,60,'老客户转介绍',3,2,1,'admin'),
(1002,'KH20260002','市生态环境局','市生态环境局',20,'政府','刘科长','13900002222','江苏省','南京市','玄武区','北京东路42号','A',95,1000000,90,'公开招标',3,2,1,'admin'),
(1003,'KH20260003','长江钢结构有限公司','长江钢构',10,'机械制造','马经理','13900003333','江苏省','南京市','六合区','中山工业园','B',70,100000,30,'展会',NULL,1,1,'admin'),
(1004,'KH20260004','个人客户-李先生','李先生',30,'民用装修','李先生','13900004444','江苏省','南京市','鼓楼区','中山北路200号','C',60,0,0,'小程序自助',NULL,1,1,'admin');

INSERT INTO biz_customer_qualification(id,customer_id,qual_type,cert_no,valid_from,valid_to,valid_status,file_name) VALUES
(1,1001,'营业执照','91320100MA1XXXXXX1','2020-01-01','2030-01-01',1,'华东制药-营业执照.pdf'),
(2,1001,'排污许可证','PWX20210001','2023-06-01','2026-12-31',2,'排污许可证.pdf'),
(3,1002,'事业单位法人证书','12320100MBXXXXXX','2019-03-01','2029-03-01',1,'法人证书.pdf');

-- 示例合同
INSERT INTO biz_contract(id,code,name,customer_id,quote_id,amount,tax_rate,sign_date,effective_date,expiry_date,
 payment_method,our_party,counter_party,status,received_amount,create_by)
VALUES
(2001,'HT20260001','2026年度环境检测框架合同',1001,NULL,280000,6,'2026-01-10','2026-01-15','2027-01-14','季度结算','某某环境检测有限公司','华东制药股份有限公司','EXECUTING',84000,'admin');

INSERT INTO biz_contract_payment(contract_id,pay_type,plan_date,occur_date,amount,invoice_no,remark) VALUES
(2001,1,'2026-04-01',NULL,70000,NULL,'Q1回款计划'),
(2001,1,'2026-07-01',NULL,70000,NULL,'Q2回款计划'),
(2001,2,NULL,'2026-04-10',84000,NULL,'Q1实际收款'),
(2001,3,NULL,'2026-04-12',84000,'FP20260412001','Q1发票');

-- 示例报价单（已审批）
INSERT INTO biz_quote(id,code,title,customer_id,pricing_mode,total_amount,discount_amount,final_amount,urgent_factor,valid_until,status,approved_at,create_by)
VALUES
(3001,'BJ20260001','废水废气例行检测报价',1001,'RULE',32400,0,32400,1.00,'2026-10-31','APPROVED','2026-08-20 10:00:00','admin');
INSERT INTO biz_quote_item(quote_id,item_name,standard_code,spec,unit,qty,unit_price,amount,formula,sort_no) VALUES
(3001,'水质pH值','HJ 1147-2020','废水总排口','点次',12,50,600,'qty * unit_price',1),
(3001,'水质化学需氧量(COD)','HJ 828-2017','废水总排口','点次',12,180,2052,'qty * unit_price * 0.95',2),
(3001,'有组织废气颗粒物','GB/T 16157-1996','1#排气筒','点位',4,1200,4800,'points * unit_price * complexity',3),
(3001,'无组织废气非甲烷总烃','HJ 38-2017','厂界4点','点次',20,300,6000,'qty * unit_price',4);

-- 示例委托单（采样中）
INSERT INTO biz_entrust_order(id,code,title,customer_id,contract_id,quote_id,entrust_type,urgency,status,
 sample_source,contact_person,contact_phone,province,city,district,sampling_address,lng,lat,
 planned_sampling_time,expected_report_date,total_amount,adjusted_amount,reviewer_id,review_time,review_opinion,
 accepted_at,sampling_at,create_by)
VALUES
(4001,'WT-2026-0001','华东制药9月废水废气例行检测',1001,2001,3001,'ENTRUST','NORMAL','SAMPLING',
 1,'陈工','13900001111','江苏省','南京市','江宁区','科学园药谷路8号',118.8621000,31.9473000,
 '2026-09-15 09:00:00','2026-09-25',32400,32400,4,'2026-09-05 14:00:00','资质在有效期内,具备CMA资质及检测能力,方法标准现行有效,采样检测资源可满足期望工期。',
 '2026-09-05 15:00:00','2026-09-10 09:00:00','admin');

INSERT INTO biz_entrust_item(order_id,item_name,standard_code,standard_name,sample_name,sample_qty,qty,unit_price,amount,is_subcontract,sort_no) VALUES
(4001,'水质pH值','HJ 1147-2020','水质 pH值的测定 电极法','废水',12,12,50,600,0,1),
(4001,'水质化学需氧量(COD)','HJ 828-2017','化学需氧量的测定 重铬酸盐法','废水',12,12,180,2052,0,2),
(4001,'有组织废气颗粒物','GB/T 16157-1996','固定污染源排气中颗粒物测定','有组织废气',4,4,1200,4800,0,3),
(4001,'二噁英类','HJ 77.2-2008','环境空气和废气 二噁英类的测定','有组织废气',2,2,12000,24000,1,4);

INSERT INTO biz_entrust_sampling_point(order_id,name,lng,lat,addr_desc,sort_no) VALUES
(4001,'废水总排口',118.8619200,31.9472500,'厂区污水站排口',1),
(4001,'1#锅炉排气筒',118.8624500,31.9476100,'锅炉房楼顶',2);

INSERT INTO biz_entrust_subcontract(order_id,item_id,subcontractor,qual_cert,amount,reason,status,create_by)
VALUES(4001,NULL,'省环境检测中心(合作实验室)','221000000001',18000,'本实验室无二噁英类CMA资质能力','APPROVED','admin');

INSERT INTO biz_entrust_status_log(order_id,from_status,to_status,action,operator_id,operator_name,remark) VALUES
(4001,NULL,'DRAFT','CREATE',1,'系统管理员','创建委托单'),
(4001,'DRAFT','REVIEWING','SUBMIT',1,'系统管理员','提交合同评审'),
(4001,'REVIEWING','ACCEPTED','REVIEW_APPROVE',4,'王评审员','评审通过,受理'),
(4001,'ACCEPTED','SAMPLING','START_SAMPLING',6,'钱采样员','采样队出发');
