USE after_sales;
INSERT INTO system_user(username,password_hash,display_name,phone,role_type,status) VALUES
('warehouse','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92','区域仓库','13800000003','WAREHOUSE','ACTIVE'),
('admin','8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92','平台管理员','13800000004','ADMIN','ACTIVE');
INSERT INTO device_type(name,status) VALUES('计算机','ACTIVE'),('打印设备','ACTIVE'),('家用电器','ACTIVE');
INSERT INTO fault_type(device_type_id,name,status) VALUES
(1,'无法开机','ACTIVE'),
(1,'系统异常','ACTIVE'),
(2,'无法打印','ACTIVE'),
(3,'制冷异常','ACTIVE'),
(3,'加热异常','ACTIVE'),
(3,'通电异常','ACTIVE'),
(3,'异响或漏水','ACTIVE');
INSERT INTO service_area(name,status) VALUES('软件园区','ACTIVE'),('高新区','ACTIVE'),('中心城区','ACTIVE');
INSERT INTO standard_time_slot(name,start_time,end_time,status) VALUES('上午','09:00:00','11:00:00','ACTIVE'),('下午一','13:00:00','15:00:00','ACTIVE'),('下午二','15:30:00','17:30:00','ACTIVE');
INSERT INTO part(part_code,name,category,model,unit,reference_price,status) VALUES
('P-CPU-FAN','CPU散热风扇','计算机配件','通用12V','个',65.00,'ACTIVE'),
('P-POWER','开关电源','电源模块','ATX-500W','个',220.00,'ACTIVE'),
('P-ROLLER','搓纸轮','打印机配件','通用型','个',35.00,'ACTIVE'),
('P-MAGNETRON','微波炉磁控管','家电配件','通用型','个',180.00,'ACTIVE'),
('P-HV-DIODE','高压二极管','家电配件','微波炉通用','个',25.00,'ACTIVE');
INSERT INTO part_inventory(part_id,total_quantity,available_quantity,locked_quantity,issued_quantity,warning_threshold) VALUES
(1,20,20,0,0,5),(2,8,8,0,0,3),(3,15,15,0,0,5),(4,10,10,0,0,3),(5,20,20,0,0,5);
INSERT INTO system_config(config_key,config_value,description,updated_by) VALUES
('CANCEL_HOURS','2','预约开始前可取消小时数',2),
('RESCHEDULE_HOURS','24','待改约超时时长',2),
('APPOINTMENT_REMINDER_HOURS','12','预约临期提醒阈值',2),
('ORDER_SLA_HOURS','48','工单状态处理阈值',2),
('PART_REVIEW_SLA_HOURS','8','配件审核提醒阈值',2);
