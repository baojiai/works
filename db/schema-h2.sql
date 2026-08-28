CREATE TABLE system_user (
 user_id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, password_hash CHAR(64) NOT NULL,
 display_name VARCHAR(80) NOT NULL, phone VARCHAR(30) UNIQUE, role_type VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE customer_profile (customer_id BIGINT PRIMARY KEY, address VARCHAR(255), CONSTRAINT fk_customer_user FOREIGN KEY(customer_id) REFERENCES system_user(user_id));
CREATE TABLE engineer_profile (
 engineer_id BIGINT PRIMARY KEY, bio VARCHAR(500), qualification_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED', employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
 completed_count INT NOT NULL DEFAULT 0, average_rating DECIMAL(3,2) NOT NULL DEFAULT 0, review_count INT NOT NULL DEFAULT 0, fulfillment_rate DECIMAL(5,2) NOT NULL DEFAULT 100,
 CONSTRAINT fk_engineer_user FOREIGN KEY(engineer_id) REFERENCES system_user(user_id)
);
CREATE TABLE device_type (device_type_id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(80) NOT NULL UNIQUE, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE');
CREATE TABLE fault_type (fault_type_id BIGINT AUTO_INCREMENT PRIMARY KEY, device_type_id BIGINT NOT NULL, name VARCHAR(80) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', FOREIGN KEY(device_type_id) REFERENCES device_type(device_type_id));
CREATE TABLE service_area (service_area_id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(80) NOT NULL UNIQUE, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE');
CREATE TABLE engineer_skill (engineer_id BIGINT NOT NULL, fault_type_id BIGINT NOT NULL, proficiency_level INT NOT NULL DEFAULT 3, PRIMARY KEY(engineer_id,fault_type_id), FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id), FOREIGN KEY(fault_type_id) REFERENCES fault_type(fault_type_id));
CREATE TABLE engineer_service_area (engineer_id BIGINT NOT NULL, service_area_id BIGINT NOT NULL, PRIMARY KEY(engineer_id,service_area_id), FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id), FOREIGN KEY(service_area_id) REFERENCES service_area(service_area_id));
CREATE TABLE standard_time_slot (slot_id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(40) NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE');
CREATE TABLE engineer_schedule (
 schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY, engineer_id BIGINT NOT NULL, service_date DATE NOT NULL, slot_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', version_no INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(engineer_id,service_date,slot_id), FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id), FOREIGN KEY(slot_id) REFERENCES standard_time_slot(slot_id)
);
CREATE TABLE repair_request (
 repair_request_id BIGINT AUTO_INCREMENT PRIMARY KEY, customer_id BIGINT NOT NULL, device_type_id BIGINT NOT NULL, fault_type_id BIGINT NOT NULL, service_area_id BIGINT NOT NULL,
 fault_description VARCHAR(1000) NOT NULL, service_address VARCHAR(255) NOT NULL, contact_phone VARCHAR(30) NOT NULL, expected_date DATE NOT NULL, expected_slot_id BIGINT, status VARCHAR(20) NOT NULL DEFAULT 'OPEN', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(customer_id) REFERENCES customer_profile(customer_id), FOREIGN KEY(device_type_id) REFERENCES device_type(device_type_id), FOREIGN KEY(fault_type_id) REFERENCES fault_type(fault_type_id), FOREIGN KEY(service_area_id) REFERENCES service_area(service_area_id), FOREIGN KEY(expected_slot_id) REFERENCES standard_time_slot(slot_id)
);
CREATE TABLE appointment (
 appointment_id BIGINT AUTO_INCREMENT PRIMARY KEY, appointment_no VARCHAR(40) NOT NULL UNIQUE, request_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, engineer_id BIGINT NOT NULL, schedule_id BIGINT NOT NULL,
 status VARCHAR(30) NOT NULL DEFAULT 'BOOKED', previous_appointment_id BIGINT, cancel_reason VARCHAR(500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(request_id) REFERENCES repair_request(repair_request_id), FOREIGN KEY(customer_id) REFERENCES customer_profile(customer_id), FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id), FOREIGN KEY(schedule_id) REFERENCES engineer_schedule(schedule_id), FOREIGN KEY(previous_appointment_id) REFERENCES appointment(appointment_id)
);
CREATE TABLE appointment_change (
 change_id BIGINT AUTO_INCREMENT PRIMARY KEY, appointment_id BIGINT NOT NULL, change_type VARCHAR(30) NOT NULL, old_status VARCHAR(30), new_status VARCHAR(30) NOT NULL, operator_id BIGINT NOT NULL, reason VARCHAR(500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(appointment_id) REFERENCES appointment(appointment_id), FOREIGN KEY(operator_id) REFERENCES system_user(user_id)
);
CREATE TABLE repair_order (
 order_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_no VARCHAR(40) NOT NULL UNIQUE, appointment_id BIGINT NOT NULL UNIQUE, customer_id BIGINT NOT NULL, engineer_id BIGINT NOT NULL, order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VISIT',
 started_at TIMESTAMP, submitted_at TIMESTAMP, completed_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(appointment_id) REFERENCES appointment(appointment_id), FOREIGN KEY(customer_id) REFERENCES customer_profile(customer_id), FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id)
);
CREATE TABLE repair_record (
 record_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, engineer_id BIGINT NOT NULL, diagnosis VARCHAR(1000) NOT NULL, repair_action VARCHAR(1000) NOT NULL, labor_hours DECIMAL(6,2) NOT NULL DEFAULT 0, remark VARCHAR(1000), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(order_id) REFERENCES repair_order(order_id), FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id)
);
CREATE TABLE order_status_log (
 log_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, operator_id BIGINT NOT NULL, old_status VARCHAR(30), new_status VARCHAR(30) NOT NULL, reason VARCHAR(500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(order_id) REFERENCES repair_order(order_id), FOREIGN KEY(operator_id) REFERENCES system_user(user_id)
);
CREATE TABLE part (part_id BIGINT AUTO_INCREMENT PRIMARY KEY, part_code VARCHAR(50) NOT NULL UNIQUE, name VARCHAR(100) NOT NULL, category VARCHAR(80), model VARCHAR(80), unit VARCHAR(20) NOT NULL, reference_price DECIMAL(10,2), status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE');
CREATE TABLE part_inventory (
 part_id BIGINT PRIMARY KEY, total_quantity INT NOT NULL DEFAULT 0, available_quantity INT NOT NULL DEFAULT 0, locked_quantity INT NOT NULL DEFAULT 0, issued_quantity INT NOT NULL DEFAULT 0, warning_threshold INT NOT NULL DEFAULT 5,
 CHECK(total_quantity>=0 AND available_quantity>=0 AND locked_quantity>=0 AND total_quantity=available_quantity+locked_quantity), FOREIGN KEY(part_id) REFERENCES part(part_id)
);
CREATE TABLE part_request (
 part_request_id BIGINT AUTO_INCREMENT PRIMARY KEY, request_no VARCHAR(40) NOT NULL UNIQUE, order_id BIGINT NOT NULL, engineer_id BIGINT NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'PENDING', reason VARCHAR(500) NOT NULL,
 reviewer_id BIGINT, reviewed_at TIMESTAMP, review_comment VARCHAR(500), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(order_id) REFERENCES repair_order(order_id), FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id), FOREIGN KEY(reviewer_id) REFERENCES system_user(user_id)
);
CREATE TABLE part_request_item (
 item_id BIGINT AUTO_INCREMENT PRIMARY KEY, part_request_id BIGINT NOT NULL, part_id BIGINT NOT NULL, request_quantity INT NOT NULL, issued_quantity INT NOT NULL DEFAULT 0, return_quantity INT NOT NULL DEFAULT 0,
 CHECK(request_quantity>0 AND issued_quantity>=0 AND return_quantity>=0 AND issued_quantity<=request_quantity AND return_quantity<=issued_quantity), UNIQUE(part_request_id,part_id), FOREIGN KEY(part_request_id) REFERENCES part_request(part_request_id), FOREIGN KEY(part_id) REFERENCES part(part_id)
);
CREATE TABLE inventory_flow (
 flow_id BIGINT AUTO_INCREMENT PRIMARY KEY, part_id BIGINT NOT NULL, order_id BIGINT, part_request_id BIGINT, flow_type VARCHAR(20) NOT NULL, quantity INT NOT NULL, reason VARCHAR(500) NOT NULL, operator_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(part_id) REFERENCES part(part_id), FOREIGN KEY(order_id) REFERENCES repair_order(order_id), FOREIGN KEY(part_request_id) REFERENCES part_request(part_request_id), FOREIGN KEY(operator_id) REFERENCES system_user(user_id)
);
CREATE TABLE acceptance (
 acceptance_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, result VARCHAR(20) NOT NULL, comment VARCHAR(1000), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(order_id) REFERENCES repair_order(order_id), FOREIGN KEY(customer_id) REFERENCES customer_profile(customer_id)
);
CREATE TABLE rework (rework_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL, acceptance_id BIGINT NOT NULL, reason VARCHAR(1000) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(order_id) REFERENCES repair_order(order_id), FOREIGN KEY(acceptance_id) REFERENCES acceptance(acceptance_id));
CREATE TABLE review (
 review_id BIGINT AUTO_INCREMENT PRIMARY KEY, order_id BIGINT NOT NULL UNIQUE, customer_id BIGINT NOT NULL, engineer_id BIGINT NOT NULL, rating INT NOT NULL, content VARCHAR(1000), status VARCHAR(20) NOT NULL DEFAULT 'VALID', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK(rating BETWEEN 1 AND 5), FOREIGN KEY(order_id) REFERENCES repair_order(order_id), FOREIGN KEY(customer_id) REFERENCES customer_profile(customer_id), FOREIGN KEY(engineer_id) REFERENCES engineer_profile(engineer_id)
);
CREATE TABLE notification (
 notification_id BIGINT AUTO_INCREMENT PRIMARY KEY, receiver_id BIGINT NOT NULL, notification_type VARCHAR(40) NOT NULL, title VARCHAR(120) NOT NULL, content VARCHAR(1000) NOT NULL, related_business_type VARCHAR(30), related_business_id BIGINT, is_read BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(receiver_id) REFERENCES system_user(user_id)
);
CREATE TABLE engineer_application (
 application_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, real_name VARCHAR(80) NOT NULL, id_card_no VARCHAR(40) NOT NULL, phone VARCHAR(30) NOT NULL,
 service_area_id BIGINT NOT NULL, experience_years INT NOT NULL DEFAULT 0, certificate_no VARCHAR(80), skill_description VARCHAR(1000) NOT NULL, material_description VARCHAR(1000) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING', reviewer_id BIGINT, review_comment VARCHAR(500), reviewed_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(user_id) REFERENCES system_user(user_id), FOREIGN KEY(service_area_id) REFERENCES service_area(service_area_id), FOREIGN KEY(reviewer_id) REFERENCES system_user(user_id)
);
CREATE TABLE engineer_application_skill (
 application_id BIGINT NOT NULL, fault_type_id BIGINT NOT NULL, PRIMARY KEY(application_id,fault_type_id),
 FOREIGN KEY(application_id) REFERENCES engineer_application(application_id), FOREIGN KEY(fault_type_id) REFERENCES fault_type(fault_type_id)
);
CREATE TABLE operation_log (
 operation_log_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, operation_type VARCHAR(40) NOT NULL, business_type VARCHAR(30), business_id BIGINT, description VARCHAR(1000), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(user_id) REFERENCES system_user(user_id)
);
CREATE TABLE system_config (config_key VARCHAR(80) PRIMARY KEY, config_value VARCHAR(200) NOT NULL, description VARCHAR(255), updated_by BIGINT, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(updated_by) REFERENCES system_user(user_id));
