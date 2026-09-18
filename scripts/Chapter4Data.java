import java.sql.*;
import java.time.LocalDate;

/** Idempotent, screenshot-oriented data for Chapter 4 figures. */
public class Chapter4Data {
    private static final String URL = System.getProperty("chapter4.db.url", "jdbc:mysql://localhost:3306/after_sales?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true");
    private static final String USER = "after_sales_app";
    private static final String PASSWORD = "123456";
    private static final String HASH = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection(URL, USER, PASSWORD)) {
            c.setAutoCommit(false);
            try {
                long admin = id(c, "SELECT user_id FROM system_user WHERE username='admin'");
                long warehouse = id(c, "SELECT user_id FROM system_user WHERE username='warehouse'");
                long customer = user(c, "13900004001", "张女士", "CUSTOMER");
                long eng1 = user(c, "13900004011", "李明工程师", "ENGINEER");
                long eng2 = user(c, "13900004012", "王强工程师", "ENGINEER");
                long eng3 = user(c, "13900004013", "陈晨工程师", "ENGINEER");
                long applicant = user(c, "13900004021", "赵工", "CUSTOMER");

                up(c, "INSERT INTO customer_profile(customer_id,address) VALUES(?,?) ON DUPLICATE KEY UPDATE address=VALUES(address)", customer, "高新区创新大道88号 3栋1202室");
                up(c, "INSERT INTO customer_profile(customer_id,address) VALUES(?,?) ON DUPLICATE KEY UPDATE address=VALUES(address)", applicant, "中心城区建设路18号");

                engineer(c, eng1, "8年现场维修经验，擅长计算机电源与散热故障", "4.92", 58, 126, "98.60");
                engineer(c, eng2, "5年企业设备维保经验，响应迅速", "4.86", 41, 93, "97.80");
                engineer(c, eng3, "原厂认证工程师，专注主机硬件诊断", "4.78", 29, 67, "96.50");

                long device = id(c, "SELECT device_type_id FROM device_type WHERE name='计算机'");
                long fault = id(c, "SELECT fault_type_id FROM fault_type WHERE device_type_id=? AND name='无法开机'", device);
                long area = id(c, "SELECT service_area_id FROM service_area WHERE name='高新区'");
                long slot1 = id(c, "SELECT slot_id FROM standard_time_slot WHERE start_time='09:00:00'");
                long slot2 = id(c, "SELECT slot_id FROM standard_time_slot WHERE start_time='13:00:00'");
                long slot3 = id(c, "SELECT slot_id FROM standard_time_slot WHERE start_time='15:30:00'");
                for (long e : new long[]{eng1, eng2, eng3}) {
                    up(c, "INSERT IGNORE INTO engineer_skill(engineer_id,fault_type_id,proficiency_level) VALUES(?,?,5)", e, fault);
                    up(c, "INSERT IGNORE INTO engineer_service_area(engineer_id,service_area_id) VALUES(?,?)", e, area);
                }

                LocalDate d1 = LocalDate.now().plusDays(2);
                LocalDate d2 = LocalDate.now().plusDays(3);
                LocalDate d3 = LocalDate.now().plusDays(4);
                long e1Busy = schedule(c, eng1, d1, slot1, "OCCUPIED");
                schedule(c, eng1, d1, slot2, "AVAILABLE");
                schedule(c, eng1, d1, slot3, "CLOSED");
                schedule(c, eng1, d2, slot1, "AVAILABLE");
                long e2Available = schedule(c, eng2, d2, slot2, "AVAILABLE");
                long e3Available = schedule(c, eng3, d2, slot3, "AVAILABLE");
                long e3PartRequest = schedule(c, eng3, d1, slot2, "OCCUPIED");
                long e1Appt = schedule(c, eng1, d3, slot1, "OCCUPIED");
                long e2Accept = schedule(c, eng2, d3, slot2, "OCCUPIED");

                long candidateReq = request(c, "CH4-CANDIDATE", customer, device, fault, area, d2, null, "台式电脑按下电源键无反应，电源指示灯不亮，急需上门检修");
                long repairingReq = request(c, "CH4-REPAIRING", customer, device, fault, area, d1, slot1, "办公电脑突然断电后无法开机，已检查插座和电源线");
                long partPendingReq = request(c, "CH4-PART-PENDING", customer, device, fault, area, d1, slot2, "备用办公电脑电源模块损坏，工程师已发起配件申请");
                long activeReq = request(c, "CH4-ACTIVE", customer, device, fault, area, d3, slot1, "设计工作站启动无响应，需要工程师上门检测电源模块");
                long acceptReq = request(c, "CH4-ACCEPT", customer, device, fault, area, d3, slot2, "电脑频繁自动关机并伴随风扇异响");

                long repairingAppt = appointment(c, "AP-CH4-REPAIR", repairingReq, customer, eng1, e1Busy, "BOOKED");
                long partPendingAppt = appointment(c, "AP-CH4-PART-PENDING", partPendingReq, customer, eng3, e3PartRequest, "BOOKED");
                long activeAppt = appointment(c, "AP-CH4-ACTIVE", activeReq, customer, eng1, e1Appt, "BOOKED");
                long acceptAppt = appointment(c, "AP-CH4-ACCEPT", acceptReq, customer, eng2, e2Accept, "BOOKED");
                long repairingOrder = order(c, "WO-CH4-REPAIR", repairingAppt, customer, eng1, "REPAIRING");
                long partPendingOrder = order(c, "WO-CH4-PART-PENDING", partPendingAppt, customer, eng3, "WAITING_PARTS");
                long activeOrder = order(c, "WO-CH4-ACTIVE", activeAppt, customer, eng1, "PENDING_VISIT");
                long acceptOrder = order(c, "WO-CH4-ACCEPT", acceptAppt, customer, eng2, "PENDING_ACCEPTANCE");

                up(c, "INSERT INTO repair_record(order_id,engineer_id,diagnosis,repair_action,labor_hours,remark) SELECT ?,?,?,?,?,? WHERE NOT EXISTS (SELECT 1 FROM repair_record WHERE order_id=? AND diagnosis LIKE '初步诊断%')",
                        repairingOrder, eng1, "初步诊断：ATX电源输出电压异常，CPU散热风扇轴承磨损", "完成电源回路检测，清洁主板积尘并更换散热风扇，待配件到位后复测", 1.5, "已向客户说明检测结果", repairingOrder);
                up(c, "INSERT INTO repair_record(order_id,engineer_id,diagnosis,repair_action,labor_hours,remark) SELECT ?,?,?,?,?,? WHERE NOT EXISTS (SELECT 1 FROM repair_record WHERE order_id=? AND diagnosis LIKE '完工诊断%')",
                        acceptOrder, eng2, "完工诊断：CPU散热风扇积尘严重并导致温度保护关机", "完成深度清洁、重新涂覆导热硅脂并进行连续两小时压力测试", 2.0, "运行稳定，已向客户演示验收", acceptOrder);
                up(c, "INSERT INTO order_status_log(order_id,operator_id,old_status,new_status,reason) SELECT ?,?,'PENDING_VISIT','REPAIRING','工程师到场并开始检测' WHERE NOT EXISTS (SELECT 1 FROM order_status_log WHERE order_id=? AND new_status='REPAIRING')", repairingOrder, eng1, repairingOrder);
                up(c, "INSERT INTO order_status_log(order_id,operator_id,old_status,new_status,reason) SELECT ?,?,'REPAIRING','PENDING_ACCEPTANCE','维修完成，提交客户验收' WHERE NOT EXISTS (SELECT 1 FROM order_status_log WHERE order_id=? AND new_status='PENDING_ACCEPTANCE')", acceptOrder, eng2, acceptOrder);

                // Keep the warehouse-review example on its own order so the interactive engineer order
                // remains able to submit a new request (only one pending request per order is allowed).
                long pendingPr = partRequest(c, "PR-CH4-PENDING", partPendingOrder, eng3, "检测确认电源模块损坏且散热风扇异响，申请更换配件", "PENDING", null, null);
                item(c, pendingPr, id(c, "SELECT part_id FROM part WHERE part_code='P-POWER'"), 1, 0);
                item(c, pendingPr, id(c, "SELECT part_id FROM part WHERE part_code='P-CPU-FAN'"), 1, 0);
                long approvedPr = partRequest(c, "PR-CH4-APPROVED", repairingOrder, eng1, "主板供电回路复测需要备用电源组件", "APPROVED", warehouse, "库存充足，审核通过并已锁定");
                long powerPart = id(c, "SELECT part_id FROM part WHERE part_code='P-POWER'");
                item(c, approvedPr, powerPart, 1, 0);

                long fanPart = id(c, "SELECT part_id FROM part WHERE part_code='P-CPU-FAN'");
                long rollerPart = id(c, "SELECT part_id FROM part WHERE part_code='P-ROLLER'");
                up(c, "UPDATE part_inventory SET total_quantity=24,available_quantity=23,locked_quantity=1,issued_quantity=6 WHERE part_id=?", fanPart);
                up(c, "UPDATE part_inventory SET total_quantity=9,available_quantity=8,locked_quantity=1,issued_quantity=3 WHERE part_id=?", powerPart);
                up(c, "UPDATE part_inventory SET total_quantity=18,available_quantity=18,locked_quantity=0,issued_quantity=4 WHERE part_id=?", rollerPart);
                flow(c, fanPart, null, null, "IN", 5, "季度常用配件补充入库", warehouse);
                flow(c, powerPart, repairingOrder, approvedPr, "LOCK", 1, "审核通过，锁定申请配件", warehouse);
                flow(c, rollerPart, null, null, "ADJUST", 2, "月度盘点库存调整", warehouse);
                flow(c, fanPart, repairingOrder, approvedPr, "OUT", 1, "维修工单配件出库", warehouse);

                up(c, "DELETE FROM engineer_application_skill WHERE application_id IN (SELECT application_id FROM engineer_application WHERE user_id=?)", applicant);
                up(c, "DELETE FROM engineer_application WHERE user_id=?", applicant);
                up(c, "INSERT INTO engineer_application(user_id,real_name,id_card_no,phone,service_area_id,experience_years,certificate_no,skill_description,material_description,status) VALUES(?,?,?,?,?,6,?,?,?,'PENDING')",
                        applicant, "赵志远", "320101199001010018", "13900004021", area, "CERT-2026-0911", "擅长计算机主板、电源及打印设备故障诊断", "身份证明、职业资格证书、近三年项目经历齐全");
                long appId = id(c, "SELECT application_id FROM engineer_application WHERE user_id=? ORDER BY application_id DESC LIMIT 1", applicant);
                up(c, "INSERT IGNORE INTO engineer_application_skill(application_id,fault_type_id) VALUES(?,?)", appId, fault);

                // A visible exception appointment for the administrator page.
                long exceptionReq = request(c, "CH4-EXCEPTION", customer, device, fault, area, d2, slot2, "工程师临时异常取消，等待客户重新选择服务时间");
                long exceptionAppt = appointment(c, "AP-CH4-EXCEPTION", exceptionReq, customer, eng2, e2Available, "PENDING_RESCHEDULE");
                order(c, "WO-CH4-EXCEPTION", exceptionAppt, customer, eng2, "CANCELLED");
                up(c, "UPDATE appointment SET cancel_reason='工程师突发设备故障无法按时上门，请客户改约',updated_at=DATE_SUB(NOW(),INTERVAL 2 HOUR) WHERE appointment_id=?", exceptionAppt);

                up(c, "DELETE FROM notification WHERE receiver_id=? AND notification_type IN ('PART_APPROVED','PART_ISSUED','ENGINEER_CANCEL','ORDER_SUBMITTED')", customer);
                notification(c, customer, "PART_APPROVED", "配件申请审核通过", "维修工单 WO-CH4-REPAIR 的配件申请已审核通过，库存已锁定。", "PART_REQUEST", approvedPr, false);
                notification(c, customer, "PART_ISSUED", "维修配件已出库", "CPU散热风扇与电源组件已完成出库，工程师将继续维修。", "PART_REQUEST", approvedPr, true);
                notification(c, customer, "ENGINEER_CANCEL", "工程师异常取消，待您改约", "原预约因工程师临时异常取消，请进入预约管理重新选择时间。", "APPOINTMENT", exceptionAppt, false);
                notification(c, customer, "ORDER_SUBMITTED", "维修完成，等待验收", "工单 WO-CH4-ACCEPT 已提交完工，请验收并评价本次服务。", "ORDER", acceptOrder, false);

                up(c, "INSERT INTO operation_log(user_id,operation_type,business_type,business_id,description) SELECT ?,?,?,?,? WHERE NOT EXISTS (SELECT 1 FROM operation_log WHERE operation_type='CH4_SEED' AND description=?)",
                        admin, "CH4_SEED", "SYSTEM", 0, "准备第4章系统截图演示数据", "准备第4章系统截图演示数据");
                c.commit();
                System.out.println("CHAPTER4_DATA_READY customer=13900004001 engineer=13900004011 candidateRequest=" + candidateReq + " repairingOrder=" + repairingOrder + " acceptOrder=" + acceptOrder);
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static long user(Connection c, String phone, String name, String role) throws Exception {
        // username and phone deliberately use the same value. Updating username on a duplicate phone
        // also migrates accounts created by older versions of this script (chapter4_* usernames).
        up(c, "INSERT INTO system_user(username,password_hash,display_name,phone,role_type,status) VALUES(?,?,?,?,?,'ACTIVE') ON DUPLICATE KEY UPDATE username=VALUES(username),password_hash=VALUES(password_hash),display_name=VALUES(display_name),phone=VALUES(phone),role_type=VALUES(role_type),status='ACTIVE'", phone, HASH, name, phone, role);
        return id(c, "SELECT user_id FROM system_user WHERE phone=?", phone);
    }
    private static void engineer(Connection c,long id,String bio,String rating,int reviews,int completed,String rate)throws Exception{
        up(c,"INSERT INTO engineer_profile(engineer_id,bio,qualification_status,employment_status,completed_count,average_rating,review_count,fulfillment_rate) VALUES(?,?,'APPROVED','ACTIVE',?,?,?,?) ON DUPLICATE KEY UPDATE bio=VALUES(bio),qualification_status='APPROVED',employment_status='ACTIVE',completed_count=VALUES(completed_count),average_rating=VALUES(average_rating),review_count=VALUES(review_count),fulfillment_rate=VALUES(fulfillment_rate)",id,bio,completed,rating,reviews,rate);
    }
    private static long schedule(Connection c,long e,LocalDate d,long slot,String status)throws Exception{
        up(c,"INSERT INTO engineer_schedule(engineer_id,service_date,slot_id,status) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE status=VALUES(status)",e,Date.valueOf(d),slot,status);
        return id(c,"SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=? AND slot_id=?",e,Date.valueOf(d),slot);
    }
    private static long request(Connection c,String marker,long customer,long device,long fault,long area,LocalDate date,Long slot,String description)throws Exception{
        Long existing=nullableId(c,"SELECT repair_request_id FROM repair_request WHERE customer_id=? AND service_address=?",customer,"高新区创新大道88号-"+marker);
        if(existing==null){
            try(PreparedStatement p=c.prepareStatement("INSERT INTO repair_request(customer_id,device_type_id,fault_type_id,service_area_id,fault_description,service_address,contact_phone,expected_date,expected_slot_id,status) VALUES(?,?,?,?,?,?,?,?,?,'OPEN')",Statement.RETURN_GENERATED_KEYS)){
                bind(p,customer,device,fault,area,description,"高新区创新大道88号-"+marker,"13900004001",Date.valueOf(date),slot);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();return r.getLong(1);}
            }
        }
        up(c,"UPDATE repair_request SET device_type_id=?,fault_type_id=?,service_area_id=?,fault_description=?,contact_phone=?,expected_date=?,expected_slot_id=?,status=IF(status='BOOKED','BOOKED','OPEN') WHERE repair_request_id=?",device,fault,area,description,"13900004001",Date.valueOf(date),slot,existing);
        return existing;
    }
    private static long appointment(Connection c,String no,long req,long customer,long eng,long sched,String status)throws Exception{
        up(c,"INSERT INTO appointment(appointment_no,request_id,customer_id,engineer_id,schedule_id,status) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE request_id=VALUES(request_id),customer_id=VALUES(customer_id),engineer_id=VALUES(engineer_id),schedule_id=VALUES(schedule_id),status=VALUES(status)",no,req,customer,eng,sched,status);
        up(c,"UPDATE repair_request SET status='BOOKED' WHERE repair_request_id=?",req);
        return id(c,"SELECT appointment_id FROM appointment WHERE appointment_no=?",no);
    }
    private static long order(Connection c,String no,long appt,long customer,long eng,String status)throws Exception{
        up(c,"INSERT INTO repair_order(order_no,appointment_id,customer_id,engineer_id,order_status,started_at,submitted_at) VALUES(?,?,?,?,?,IF(?='REPAIRING',NOW(),NULL),IF(?='PENDING_ACCEPTANCE',NOW(),NULL)) ON DUPLICATE KEY UPDATE order_status=VALUES(order_status),started_at=VALUES(started_at),submitted_at=VALUES(submitted_at)",no,appt,customer,eng,status,status,status);
        return id(c,"SELECT order_id FROM repair_order WHERE order_no=?",no);
    }
    private static long partRequest(Connection c,String no,long order,long eng,String reason,String status,Long reviewer,String comment)throws Exception{
        up(c,"INSERT INTO part_request(request_no,order_id,engineer_id,status,reason,reviewer_id,reviewed_at,review_comment) VALUES(?,?,?,?,?,?,IF(? IS NULL,NULL,NOW()),?) ON DUPLICATE KEY UPDATE order_id=VALUES(order_id),engineer_id=VALUES(engineer_id),status=VALUES(status),reason=VALUES(reason),reviewer_id=VALUES(reviewer_id),reviewed_at=VALUES(reviewed_at),review_comment=VALUES(review_comment)",no,order,eng,status,reason,reviewer,reviewer,comment);
        return id(c,"SELECT part_request_id FROM part_request WHERE request_no=?",no);
    }
    private static void item(Connection c,long req,long part,int qty,int issued)throws Exception{up(c,"INSERT INTO part_request_item(part_request_id,part_id,request_quantity,issued_quantity) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE request_quantity=VALUES(request_quantity),issued_quantity=VALUES(issued_quantity)",req,part,qty,issued);}
    private static void flow(Connection c,long part,Long order,Long req,String type,int qty,String reason,long operator)throws Exception{up(c,"INSERT INTO inventory_flow(part_id,order_id,part_request_id,flow_type,quantity,reason,operator_id) SELECT ?,?,?,?,?,?,? WHERE NOT EXISTS (SELECT 1 FROM inventory_flow WHERE part_id=? AND flow_type=? AND reason=?)",part,order,req,type,qty,reason,operator,part,type,reason);}
    private static void notification(Connection c,long receiver,String type,String title,String content,String bt,long bid,boolean read)throws Exception{up(c,"INSERT INTO notification(receiver_id,notification_type,title,content,related_business_type,related_business_id,is_read) VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE title=VALUES(title),content=VALUES(content),is_read=VALUES(is_read)",receiver,type,title,content,bt,bid,read);}
    private static long id(Connection c,String sql,Object...a)throws Exception{Long v=nullableId(c,sql,a);if(v==null)throw new SQLException("No row: "+sql);return v;}
    private static Long nullableId(Connection c,String sql,Object...a)throws Exception{try(PreparedStatement p=c.prepareStatement(sql)){bind(p,a);try(ResultSet r=p.executeQuery()){return r.next()?r.getLong(1):null;}}}
    private static void up(Connection c,String sql,Object...a)throws Exception{try(PreparedStatement p=c.prepareStatement(sql)){bind(p,a);p.executeUpdate();}}
    private static void bind(PreparedStatement p,Object...a)throws Exception{for(int i=0;i<a.length;i++)p.setObject(i+1,a[i]);}
}
