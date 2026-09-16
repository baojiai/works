-- =============================================================
-- 售后维修预约与配件协同管理系统 MySQL 8 建库脚本（独立完整单文件）
-- 字符集：utf8mb4 / utf8mb4_unicode_ci；引擎：InnoDB
-- 执行：mysql -u <用户> -p < database/after_sales.sql
-- 结构与本项目 H2 业务模型（db/schema-h2.sql + db/seed-h2.sql）保持一致
-- =============================================================

CREATE DATABASE IF NOT EXISTS after_sales DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE after_sales;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS engineer_application_skill;
DROP TABLE IF EXISTS engineer_application;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS system_config;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS rework;
DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS acceptance;
DROP TABLE IF EXISTS inventory_flow;
DROP TABLE IF EXISTS part_request_item;
DROP TABLE IF EXISTS part_request;
DROP TABLE IF EXISTS part_inventory;
DROP TABLE IF EXISTS part;
DROP TABLE IF EXISTS order_status_log;
DROP TABLE IF EXISTS repair_record;
DROP TABLE IF EXISTS repair_order;
DROP TABLE IF EXISTS appointment_change;
DROP TABLE IF EXISTS appointment;
DROP TABLE IF EXISTS repair_request;
DROP TABLE IF EXISTS engineer_schedule;
DROP TABLE IF EXISTS standard_time_slot;
DROP TABLE IF EXISTS engineer_service_area;
DROP TABLE IF EXISTS engineer_skill;
DROP TABLE IF EXISTS fault_type;
DROP TABLE IF EXISTS service_area;
DROP TABLE IF EXISTS device_type;
DROP TABLE IF EXISTS engineer_profile;
DROP TABLE IF EXISTS customer_profile;
DROP TABLE IF EXISTS system_user;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE system_user (
 user_id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, password_hash CHAR(64) NOT NULL,
 display_name VARCHAR(80) NOT NULL, phone VARCHAR(30) UNIQUE, role_type VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_profile (
 customer_id BIGINT PRIMARY KEY, address VARCHAR(255),
 CONSTRAINT fk_customer_user FOREIGN KEY(customer_id) REFERENCES system_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE engineer_profile (
 engineer_id BIGINT PRIMARY KEY, bio VARCHAR(500), qualification_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED', employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 completed_count INT NOT NULL DEFAULT 0, average_rating DECIMAL(3,2) NOT NULL DEFAULT 0, review_count INT NOT NULL DEFAULT 0, fulfillment_rate DECIMAL(5,2) NOT NULL DEFAULT 100,
 CONSTRAINT fk_engineer_user FOREIGN KEY(engineer_id) REFERENCES system_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE device_type (
 device_type_id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(80) NOT NULL UNIQUE, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fault_type (
 fault_type_id BIGINT AUTO_INCREMENT PRIMARY KEY, device_type_id BIGINT NOT NULL, name VARCHAR(80) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 FOREIGN KEY(device_type_id) REFERENCES device_type(device_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE service_area (
 service_area_id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(80) NOT NULL UNIQUE, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE engineer_skill (
 engineer_id BIGINT NOT NULL, fault_type_id BIGINT NOT NULL, proficiency_level INT NOT NULL DEFAULT 3,
 PRIMARY KEY(engineer_id,fault_type_id),
 FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id),
 FOREIGN KEY(fault_type_id) REFERENCES fault_type(fault_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE engineer_service_area (
 engineer_id BIGINT NOT NULL, service_area_id BIGINT NOT NULL,
 PRIMARY KEY(engineer_id,service_area_id),
 FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id),
 FOREIGN KEY(service_area_id) REFERENCES service_area(service_area_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE standard_time_slot (
 slot_id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(40) NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE engineer_schedule (
 schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY, engineer_id BIGINT NOT NULL, service_date DATE NOT NULL, slot_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', version_no INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(engineer_id,service_date,slot_id),
 FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id),
 FOREIGN KEY(slot_id) REFERENCES standard_time_slot(slot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE repair_request (
 repair_request_id BIGINT AUTO_INCREMENT PRIMARY KEY, customer_id BIGINT NOT NULL, device_type_id BIGINT NOT NULL, fault_type_id BIGINT NOT NULL, service_area_id BIGINT NOT NULL,
 fault_description VARCHAR(1000) NOT NULL, service_address VARCHAR(255) NOT NULL, contact_phone VARCHAR(30) NOT NULL, expected_date DATE NOT NULL, expected_slot_id BIGINT, status VARCHAR(20) NOT NULL DEFAULT 'OPEN', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(customer_id) REFERENCES customer_profile(customer_id),
 FOREIGN KEY(device_type_id) REFERENCES device_type(device_type_id),
 FOREIGN KEY(fault_type_id) REFERENCES fault_type(fault_type_id),
 FOREIGN KEY(service_area_id) REFERENCES service_area(service_area_id),
 FOREIGN KEY(expected_slot_id) REFERENCES standard_time_slot(slot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE appointment (
 appointment_id BIGINT AUTO_INCREMENT PRIMARY KEY, appointment_no VARCHAR(40) NOT NULL UNIQUE, request_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, engineer_id BIGINT NOT NULL, schedule_id BIGINT NOT NULL,
 status VARCHAR(30) NOT NULL DEFAULT 'BOOKED', previous_appointment_id BIGINT, cancel_reason VARCHAR(500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(request_id) REFERENCES repair_request(repair_request_id),
 FOREIGN KEY(customer_id) REFERENCES customer_profile(customer_id),
 FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id),
 FOREIGN KEY(schedule_id) REFERENCES engineer_schedule(schedule_id),
 FOREIGN KEY(previous_appointment_id) REFERENCES appointment(appointment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE appointment_change (
 change_id BIGINT AUTO_INCREMENT PRIMARY KEY, appointment_id BIGINT NOT NULL, change_type VARCHAR(30) NOT NULL, old_status VARCHAR(30), new_status VARCHAR(30) NOT NULL, operator_id BIGINT NOT NULL, reason VARCHAR(500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(appointment_id) REFERENCES appointment(appointment_id),
 FOREIGN KEY(operator_id) REFERENCES system_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE repair_order (
 order_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_no VARCHAR(40) NOT NULL UNIQUE, appointment_id BIGINT NOT NULL UNIQUE, customer_id BIGINT NOT NULL, engineer_id BIGINT NOT NULL, order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VISIT',
 started_at TIMESTAMP, submitted_at TIMESTAMP, completed_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(appointment_id) REFERENCES appointment(appointment_id),
 FOREIGN KEY(customer_id) REFERENCES customer_profile(customer_id),
 FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE repair_record (
 record_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, engineer_id BIGINT NOT NULL, diagnosis VARCHAR(1000) NOT NULL, repair_action VARCHAR(1000) NOT NULL, labor_hours DECIMAL(6,2) NOT NULL DEFAULT 0, remark VARCHAR(1000), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(order_id) REFERENCES repair_order(order_id),
 FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_status_log (
 log_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, operator_id BIGINT NOT NULL, old_status VARCHAR(30), new_status VARCHAR(30) NOT NULL, reason VARCHAR(500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(order_id) REFERENCES repair_order(order_id),
 FOREIGN KEY(operator_id) REFERENCES system_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE part (
 part_id BIGINT AUTO_INCREMENT PRIMARY KEY, part_code VARCHAR(50) NOT NULL UNIQUE, name VARCHAR(100) NOT NULL, category VARCHAR(80), model VARCHAR(80), unit VARCHAR(20) NOT NULL, reference_price DECIMAL(10,2), status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE part_inventory (
 part_id BIGINT PRIMARY KEY, total_quantity INT NOT NULL DEFAULT 0, available_quantity INT NOT NULL DEFAULT 0, locked_quantity INT NOT NULL DEFAULT 0, issued_quantity INT NOT NULL DEFAULT 0, warning_threshold INT NOT NULL DEFAULT 5,
 CHECK(total_quantity>=0 AND available_quantity>=0 AND locked_quantity>=0 AND total_quantity=available_quantity+locked_quantity),
 FOREIGN KEY(part_id) REFERENCES part(part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE part_request (
 part_request_id BIGINT AUTO_INCREMENT PRIMARY KEY, request_no VARCHAR(40) NOT NULL UNIQUE, order_id BIGINT NOT NULL, engineer_id BIGINT NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'PENDING', reason VARCHAR(500) NOT NULL,
 reviewer_id BIGINT, reviewed_at TIMESTAMP, review_comment VARCHAR(500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(order_id) REFERENCES repair_order(order_id),
 FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id),
 FOREIGN KEY(reviewer_id) REFERENCES system_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE part_request_item (
 item_id BIGINT AUTO_INCREMENT PRIMARY KEY, part_request_id BIGINT NOT NULL, part_id BIGINT NOT NULL, request_quantity INT NOT NULL, issued_quantity INT NOT NULL DEFAULT 0, return_quantity INT NOT NULL DEFAULT 0,
 CHECK(request_quantity>0 AND issued_quantity>=0 AND return_quantity>=0 AND issued_quantity<=request_quantity AND return_quantity<=issued_quantity),
 UNIQUE(part_request_id,part_id),
 FOREIGN KEY(part_request_id) REFERENCES part_request(part_request_id),
 FOREIGN KEY(part_id) REFERENCES part(part_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inventory_flow (
 flow_id BIGINT AUTO_INCREMENT PRIMARY KEY, part_id BIGINT NOT NULL, order_id BIGINT, part_request_id BIGINT, flow_type VARCHAR(20) NOT NULL, quantity INT NOT NULL, reason VARCHAR(500) NOT NULL, operator_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(part_id) REFERENCES part(part_id),
 FOREIGN KEY(order_id) REFERENCES repair_order(order_id),
 FOREIGN KEY(part_request_id) REFERENCES part_request(part_request_id),
 FOREIGN KEY(operator_id) REFERENCES system_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE acceptance (
 acceptance_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, result VARCHAR(20) NOT NULL, comment VARCHAR(1000), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(order_id) REFERENCES repair_order(order_id),
 FOREIGN KEY(customer_id) REFERENCES customer_profile(customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rework (
 rework_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, acceptance_id BIGINT NOT NULL, reason VARCHAR(1000) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(order_id) REFERENCES repair_order(order_id),
 FOREIGN KEY(acceptance_id) REFERENCES acceptance(acceptance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review (
 review_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL UNIQUE, customer_id BIGINT NOT NULL, engineer_id BIGINT NOT NULL, rating INT NOT NULL, content VARCHAR(1000), status VARCHAR(20) NOT NULL DEFAULT 'VALID', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK(rating BETWEEN 1 AND 5),
 FOREIGN KEY(order_id) REFERENCES repair_order(order_id),
 FOREIGN KEY(customer_id) REFERENCES customer_profile(customer_id),
 FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification (
 notification_id BIGINT AUTO_INCREMENT PRIMARY KEY, receiver_id BIGINT NOT NULL, notification_type VARCHAR(40) NOT NULL, title VARCHAR(120) NOT NULL, content VARCHAR(1000) NOT NULL, related_business_type VARCHAR(30), related_business_id BIGINT, is_read BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(receiver_id) REFERENCES system_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE engineer_application (
 application_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, real_name VARCHAR(80) NOT NULL, id_card_no VARCHAR(40) NOT NULL, phone VARCHAR(30) NOT NULL,
 service_area_id BIGINT NOT NULL, experience_years INT NOT NULL DEFAULT 0, certificate_no VARCHAR(80), skill_description VARCHAR(1000) NOT NULL, material_description VARCHAR(1000) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING', reviewer_id BIGINT, review_comment VARCHAR(500), reviewed_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(user_id) REFERENCES system_user(user_id),
 FOREIGN KEY(service_area_id) REFERENCES service_area(service_area_id),
 FOREIGN KEY(reviewer_id) REFERENCES system_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE engineer_application_skill (
 application_id BIGINT NOT NULL, fault_type_id BIGINT NOT NULL,
 PRIMARY KEY(application_id,fault_type_id),
 FOREIGN KEY(application_id) REFERENCES engineer_application(application_id),
 FOREIGN KEY(fault_type_id) REFERENCES fault_type(fault_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operation_log (
 operation_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, operation_type VARCHAR(40) NOT NULL, business_type VARCHAR(30), business_id BIGINT, description VARCHAR(1000), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(user_id) REFERENCES system_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_config (
 config_key VARCHAR(80) PRIMARY KEY, config_value VARCHAR(200) NOT NULL, description VARCHAR(255), updated_by BIGINT, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(updated_by) REFERENCES system_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================
-- 种子数据（与 db/seed-h2.sql 一致，仅包含项目正式种子）
-- 初始内部账号密码均为 123456（sha256）
-- =============================================================

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
