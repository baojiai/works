import com.course.aftersales.model.SessionUser;
import com.course.aftersales.service.AdminService;
import com.course.aftersales.service.AuthService;
import com.course.aftersales.service.CustomerService;
import com.course.aftersales.service.EngineerApplicationService;
import com.course.aftersales.service.EngineerService;
import com.course.aftersales.service.NotificationService;
import com.course.aftersales.service.WarehouseService;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class Stage9MySqlTransactionProbe {
    private static String url, user, password;
    private static Connection direct;

    public static void main(String[] args) throws Exception {
        url = System.getenv("APP_DB_URL");
        user = System.getenv("APP_DB_USER");
        password = System.getenv("APP_DB_PASSWORD");
        if (url == null || user == null || password == null) throw new IllegalStateException("APP_DB_URL/USER/PASSWORD 环境变量未设置");
        Class.forName(System.getenv("APP_DB_DRIVER") != null ? System.getenv("APP_DB_DRIVER") : "com.mysql.cj.jdbc.Driver");

        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml")) {
            AuthService auth = context.getBean(AuthService.class);
            CustomerService customers = context.getBean(CustomerService.class);
            EngineerApplicationService applications = context.getBean(EngineerApplicationService.class);
            EngineerService engineers = context.getBean(EngineerService.class);
            AdminService admins = context.getBean(AdminService.class);
            WarehouseService warehouse = context.getBean(WarehouseService.class);
            NotificationService notifications = context.getBean(NotificationService.class);
            if (!AopUtils.isAopProxy(customers) || !AopUtils.isAopProxy(warehouse) || !AopUtils.isAopProxy(admins)) throw new AssertionError("services are not transactional proxies");

            cleanupProbeData();
            long adminId = scalar("SELECT user_id FROM system_user WHERE username='admin'");
            long warehouseId = scalar("SELECT user_id FROM system_user WHERE username='warehouse'");
            long areaId = scalar("SELECT MIN(service_area_id) FROM service_area WHERE status='ACTIVE'");
            long deviceId = scalar("SELECT MIN(device_type_id) FROM device_type WHERE status='ACTIVE'");
            long fault1 = scalar("SELECT MIN(fault_type_id) FROM fault_type WHERE status='ACTIVE' AND device_type_id=?", deviceId);
            long slot1 = scalar("SELECT MIN(slot_id) FROM standard_time_slot WHERE status='ACTIVE'");
            long slot2 = scalar("SELECT MIN(slot_id) FROM standard_time_slot WHERE status='ACTIVE' AND slot_id<>?", slot1);

            // ---- A. generated key + 预约失败回滚（previous_appointment_id 外键注入） ----
            SessionUser applicant = auth.registerCustomer("13977190001", "9B-probe-eng", "123456", "123456");
            if (applicant.getId() <= 0) throw new AssertionError("generated key: user id not filled");
            applications.submit(applicant.getId(), "9B-probe-eng", "ID-9B-1", "13977190001", areaId, 3, "CERT", "bio", "mat", new String[]{String.valueOf(fault1)});
            long applicationId = scalar("SELECT application_id FROM engineer_application WHERE user_id=?", applicant.getId());
            if (applicationId <= 0) throw new AssertionError("generated key: application id not filled");
            admins.reviewApplication(adminId, applicationId, true, "approve");
            long engineerId = applicant.getId();

            SessionUser customer1 = auth.registerCustomer("13977190002", "9B-probe-c1", "123456", "123456");
            Date d5 = Date.valueOf(LocalDate.now().plusDays(5));
            long requestId1 = customers.createRequest(customer1.getId(), deviceId, fault1, areaId, "9B-probe-fault", "addr", "13977190002", d5, null);
            if (requestId1 <= 0) throw new AssertionError("generated key: request id not filled");
            engineers.addSchedule(engineerId, d5, slot1);
            long schedule1 = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=?", engineerId, d5);

            expectFailure(() -> customers.book(customer1.getId(), requestId1, engineerId, schedule1, 999999999L));
            assertCount(0, "SELECT COUNT(*) FROM appointment WHERE request_id=?", requestId1, "book rollback appointment");
            assertText("AVAILABLE", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedule1, "book rollback schedule");
            assertText("OPEN", "SELECT status FROM repair_request WHERE repair_request_id=?", requestId1, "book rollback request");
            assertCount(0, "SELECT COUNT(*) FROM notification WHERE notification_type='NEW_APPOINTMENT'", "book rollback notification");

            long appointmentId1 = customers.book(customer1.getId(), requestId1, engineerId, schedule1, 0);
            if (appointmentId1 <= 0) throw new AssertionError("generated key: appointment id not filled");
            long orderId1 = scalar("SELECT order_id FROM repair_order WHERE appointment_id=?", appointmentId1);
            if (orderId1 <= 0) throw new AssertionError("generated key: order id not filled");
            assertText("OCCUPIED", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedule1, "book success schedule");

            // ---- B. 改约失败回滚（删除旧工单使 closeOldAppointment 抛错，前面多表写入必须回滚） ----
            Date d6 = Date.valueOf(LocalDate.now().plusDays(6));
            engineers.addSchedule(engineerId, d6, slot2);
            long schedule2 = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=?", engineerId, d6);
            execute("DELETE FROM order_status_log WHERE order_id=?", orderId1);
            execute("DELETE FROM repair_order WHERE order_id=?", orderId1);
            expectFailure(() -> customers.book(customer1.getId(), requestId1, engineerId, schedule2, appointmentId1));
            assertText("BOOKED", "SELECT status FROM appointment WHERE appointment_id=?", appointmentId1, "reschedule rollback old appointment");
            assertText("OCCUPIED", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedule1, "reschedule rollback old schedule");
            assertText("AVAILABLE", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedule2, "reschedule rollback new schedule");
            assertCount(1, "SELECT COUNT(*) FROM appointment WHERE request_id=?", requestId1, "reschedule rollback no new appointment");
            assertText("BOOKED", "SELECT status FROM repair_request WHERE repair_request_id=?", requestId1, "reschedule rollback request");
            assertCount(0, "SELECT COUNT(*) FROM notification WHERE notification_type='NEW_APPOINTMENT' AND related_business_id<>?", appointmentId1, "reschedule rollback no new notification");

            // ---- C. 管理员审核失败回滚（phone UNIQUE 冲突注入） ----
            SessionUser applicant2 = auth.registerCustomer("13977190003", "9B-probe-eng2", "123456", "123456");
            applications.submit(applicant2.getId(), "9B-probe-eng2", "ID-9B-2", "13977190003", areaId, 2, "CERT", "bio", "mat", new String[]{String.valueOf(fault1)});
            long applicationId2 = scalar("SELECT application_id FROM engineer_application WHERE user_id=?", applicant2.getId());
            execute("UPDATE engineer_application SET phone=? WHERE application_id=?", text("SELECT phone FROM system_user WHERE username='admin'"), applicationId2);
            expectFailure(() -> admins.reviewApplication(adminId, applicationId2, true, "approve"));
            assertText("PENDING", "SELECT status FROM engineer_application WHERE application_id=?", applicationId2, "admin review rollback application");
            assertText("CUSTOMER", "SELECT role_type FROM system_user WHERE user_id=?", applicant2.getId(), "admin review rollback role");
            assertCount(0, "SELECT COUNT(*) FROM engineer_profile WHERE engineer_id=?", applicant2.getId(), "admin review rollback profile");
            assertCount(0, "SELECT COUNT(*) FROM engineer_skill WHERE engineer_id=?", applicant2.getId(), "admin review rollback skill");
            execute("UPDATE engineer_application SET phone='13977190003' WHERE application_id=?", applicationId2);
            admins.reviewApplication(adminId, applicationId2, true, "approve");
            long engineerId2 = applicant2.getId();

            // ---- D/E. 仓库审核/出库回滚（第二配件库存不足 / locked 归零注入） ----
            long requestId2 = customers.createRequest(customer1.getId(), deviceId, fault1, areaId, "9B-probe-parts", "addr", "13977190002", d6, null);
            long appointmentId2 = customers.book(customer1.getId(), requestId2, engineerId, schedule2, 0);
            long orderId2 = scalar("SELECT order_id FROM repair_order WHERE appointment_id=?", appointmentId2);
            engineers.transition(engineerId, orderId2, "START");
            engineers.createPartRequest(engineerId, orderId2, "9B-probe-part-req", new String[]{"1", "2"}, new String[]{"2", "1"});
            long partRequestId = scalar("SELECT MAX(part_request_id) FROM part_request");
            if (partRequestId <= 0) throw new AssertionError("generated key: part_request id not filled");
            execute("UPDATE part_inventory SET total_quantity=0,available_quantity=0,locked_quantity=0 WHERE part_id=2");
            expectFailure(() -> warehouse.review(warehouseId, partRequestId, true, "approve"));
            assertText("PENDING", "SELECT status FROM part_request WHERE part_request_id=?", partRequestId, "wh review rollback request");
            assertText("20,20,0,0", "SELECT CONCAT(total_quantity,',',available_quantity,',',locked_quantity,',',issued_quantity) FROM part_inventory WHERE part_id=1", "wh review rollback part1");
            assertCount(0, "SELECT COUNT(*) FROM inventory_flow WHERE part_request_id=?", partRequestId, "wh review rollback no flows");
            execute("UPDATE part_inventory SET total_quantity=8,available_quantity=8,locked_quantity=0 WHERE part_id=2");
            warehouse.review(warehouseId, partRequestId, true, "approve");
            assertText("APPROVED", "SELECT status FROM part_request WHERE part_request_id=?", partRequestId, "wh review success");
            assertText("20,18,2,0", "SELECT CONCAT(total_quantity,',',available_quantity,',',locked_quantity,',',issued_quantity) FROM part_inventory WHERE part_id=1", "wh review part1 locked");
            assertText("8,7,1,0", "SELECT CONCAT(total_quantity,',',available_quantity,',',locked_quantity,',',issued_quantity) FROM part_inventory WHERE part_id=2", "wh review part2 locked");

            execute("UPDATE part_inventory SET available_quantity=available_quantity+1,locked_quantity=0 WHERE part_id=2");
            expectFailure(() -> warehouse.issue(warehouseId, partRequestId));
            assertText("APPROVED", "SELECT status FROM part_request WHERE part_request_id=?", partRequestId, "wh issue rollback request");
            assertText("20,18,2,0", "SELECT CONCAT(total_quantity,',',available_quantity,',',locked_quantity,',',issued_quantity) FROM part_inventory WHERE part_id=1", "wh issue rollback part1");
            assertText("0", "SELECT issued_quantity FROM part_request_item WHERE part_request_id=? ORDER BY item_id LIMIT 1", partRequestId, "wh issue rollback item issued");
            assertCount(0, "SELECT COUNT(*) FROM inventory_flow WHERE part_request_id=? AND flow_type='OUT'", partRequestId, "wh issue rollback no OUT");
            execute("UPDATE part_inventory SET available_quantity=available_quantity-1,locked_quantity=1 WHERE part_id=2");
            warehouse.issue(warehouseId, partRequestId);
            assertText("ISSUED", "SELECT status FROM part_request WHERE part_request_id=?", partRequestId, "wh issue success");
            assertText("18,18,0,2", "SELECT CONCAT(total_quantity,',',available_quantity,',',locked_quantity,',',issued_quantity) FROM part_inventory WHERE part_id=1", "wh issue part1 issued");

            // ---- complete 事务化：失败回滚 + 成功 ----
            SessionUser tempOp = auth.registerCustomer("13977190006", "9B-probe-tmpop", "123456", "123456");
            execute("DELETE FROM customer_profile WHERE customer_id=?", tempOp.getId());
            execute("DELETE FROM operation_log WHERE user_id=?", tempOp.getId());
            execute("DELETE FROM system_user WHERE user_id=?", tempOp.getId());
            expectFailure(() -> warehouse.complete(tempOp.getId(), partRequestId));
            assertText("ISSUED", "SELECT status FROM part_request WHERE part_request_id=?", partRequestId, "complete rollback request");
            assertCount(0, "SELECT COUNT(*) FROM operation_log WHERE operation_type='COMPLETE_PART_REQUEST' AND business_id=?", partRequestId, "complete rollback no log");
            warehouse.complete(warehouseId, partRequestId);
            assertText("COMPLETED", "SELECT status FROM part_request WHERE part_request_id=?", partRequestId, "complete success request");
            assertCount(1, "SELECT COUNT(*) FROM operation_log WHERE operation_type='COMPLETE_PART_REQUEST' AND business_id=?", partRequestId, "complete success log");

            // ---- F. 库存并发（同一 part3 库存15，两申请各10，同时审核只一个成功） ----
            long requestId3 = customers.createRequest(customer1.getId(), deviceId, fault1, areaId, "9B-probe-conc1", "addr", "13977190002", d6, null);
            long requestId4 = customers.createRequest(customer1.getId(), deviceId, fault1, areaId, "9B-probe-conc2", "addr", "13977190002", d6, null);
            Date d7 = Date.valueOf(LocalDate.now().plusDays(7));
            engineers.addSchedule(engineerId, d7, slot1);
            engineers.addSchedule(engineerId2, d7, slot2);
            long schedE1 = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=?", engineerId, d7);
            long schedE2 = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=?", engineerId2, d7);
            customers.book(customer1.getId(), requestId3, engineerId, schedE1, 0);
            customers.book(customer1.getId(), requestId4, engineerId2, schedE2, 0);
            long orderId3 = scalar("SELECT order_id FROM repair_order WHERE appointment_id=(SELECT appointment_id FROM appointment WHERE request_id=?)", requestId3);
            long orderId4 = scalar("SELECT order_id FROM repair_order WHERE appointment_id=(SELECT appointment_id FROM appointment WHERE request_id=?)", requestId4);
            engineers.transition(engineerId, orderId3, "START");
            engineers.transition(engineerId2, orderId4, "START");
            engineers.createPartRequest(engineerId, orderId3, "9B-probe-conc-p1", new String[]{"3"}, new String[]{"10"});
            engineers.createPartRequest(engineerId2, orderId4, "9B-probe-conc-p2", new String[]{"3"}, new String[]{"10"});
            long prConc1 = scalar("SELECT part_request_id FROM part_request WHERE order_id=? ORDER BY part_request_id DESC LIMIT 1", orderId3);
            long prConc2 = scalar("SELECT part_request_id FROM part_request WHERE order_id=? ORDER BY part_request_id DESC LIMIT 1", orderId4);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger success = new AtomicInteger();
            Future<?> f1 = pool.submit(() -> { try { latch.await(); warehouse.review(warehouseId, prConc1, true, "c1"); success.incrementAndGet(); } catch (Exception e) { } });
            Future<?> f2 = pool.submit(() -> { try { latch.await(); warehouse.review(warehouseId, prConc2, true, "c2"); success.incrementAndGet(); } catch (Exception e) { } });
            latch.countDown();
            f1.get(); f2.get(); pool.shutdown();
            if (success.get() != 1) throw new AssertionError("inventory concurrency: expected exactly 1 success, got " + success.get());
            assertText("15,5,10,0", "SELECT CONCAT(total_quantity,',',available_quantity,',',locked_quantity,',',issued_quantity) FROM part_inventory WHERE part_id=3", "inventory concurrency locked exactly 10");
            assertCount(1, "SELECT COUNT(*) FROM part_request WHERE part_request_id IN(?,?) AND status='APPROVED'", new Object[]{prConc1, prConc2}, "inventory concurrency one approved");
            long winnerId = scalar("SELECT part_request_id FROM part_request WHERE part_request_id IN(?,?) AND status='APPROVED'", new Object[]{prConc1, prConc2});
            warehouse.release(warehouseId, winnerId, "9B-probe-cleanup");
            assertText("15,15,0,0", "SELECT CONCAT(total_quantity,',',available_quantity,',',locked_quantity,',',issued_quantity) FROM part_inventory WHERE part_id=3", "inventory concurrency released");

            // ---- G. 预约并发（同一排班同时 book 只一个成功） ----
            Date d8 = Date.valueOf(LocalDate.now().plusDays(8));
            engineers.addSchedule(engineerId, d8, slot1);
            long schedConc = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=?", engineerId, d8);
            SessionUser c2 = auth.registerCustomer("13977190004", "9B-probe-c2", "123456", "123456");
            SessionUser c3 = auth.registerCustomer("13977190005", "9B-probe-c3", "123456", "123456");
            long reqA = customers.createRequest(customer1.getId(), deviceId, fault1, areaId, "9B-conc-a", "addr", "13977190002", d8, null);
            long reqB = customers.createRequest(c2.getId(), deviceId, fault1, areaId, "9B-conc-b", "addr", "13977190004", d8, null);
            long reqC = customers.createRequest(c3.getId(), deviceId, fault1, areaId, "9B-conc-c", "addr", "13977190005", d8, null);
            ExecutorService pool2 = Executors.newFixedThreadPool(2);
            CountDownLatch latch2 = new CountDownLatch(1);
            AtomicInteger bookSuccess = new AtomicInteger();
            Future<?> b1 = pool2.submit(() -> { try { latch2.await(); customers.book(customer1.getId(), reqA, engineerId, schedConc, 0); bookSuccess.incrementAndGet(); } catch (Exception e) { } });
            Future<?> b2 = pool2.submit(() -> { try { latch2.await(); customers.book(c2.getId(), reqB, engineerId, schedConc, 0); bookSuccess.incrementAndGet(); } catch (Exception e) { } });
            latch2.countDown();
            b1.get(); b2.get(); pool2.shutdown();
            if (bookSuccess.get() != 1) throw new AssertionError("booking concurrency: expected exactly 1 success, got " + bookSuccess.get());
            assertText("OCCUPIED", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedConc, "booking concurrency schedule occupied");
            assertCount(1, "SELECT COUNT(*) FROM appointment WHERE schedule_id=?", schedConc, "booking concurrency one appointment");
            assertCount(1, "SELECT COUNT(*) FROM repair_order WHERE appointment_id IN (SELECT appointment_id FROM appointment WHERE schedule_id=?)", schedConc, "booking concurrency one order");
            assertCount(1, "SELECT COUNT(*) FROM notification WHERE notification_type='NEW_APPOINTMENT' AND related_business_id IN (SELECT appointment_id FROM appointment WHERE schedule_id=?)", schedConc, "booking concurrency one notification");
            expectFailure(() -> customers.book(c3.getId(), reqC, engineerId, schedConc, 0));

            // ---- H. 时区验证 ----
            long dbMillis = scalar("SELECT UNIX_TIMESTAMP(NOW())*1000");
            long javaMillis = System.currentTimeMillis();
            if (Math.abs(dbMillis - javaMillis) > 120000) throw new AssertionError("timezone drift: db=" + dbMillis + " java=" + javaMillis);

            // ---- I. Boolean 验证 ----
            execute("INSERT INTO notification(receiver_id,notification_type,title,content,related_business_type,related_business_id,is_read) VALUES(?,'9B_PROBE','t','c',NULL,NULL,FALSE)", customer1.getId());
            execute("INSERT INTO notification(receiver_id,notification_type,title,content,related_business_type,related_business_id,is_read) VALUES(?,'9B_PROBE','t2','c2',NULL,NULL,FALSE)", customer1.getId());
            long notifId = scalar("SELECT MAX(notification_id) FROM notification");
            assertText("0", "SELECT is_read FROM notification WHERE notification_id=?", notifId, "boolean default false");
            long unreadBefore = notifications.unreadCount(customer1.getId());
            notifications.read(customer1.getId(), notifId);
            assertText("1", "SELECT is_read FROM notification WHERE notification_id=?", notifId, "boolean read true");
            if (notifications.unreadCount(customer1.getId()) != unreadBefore - 1) throw new AssertionError("unreadCount did not decrease");
            notifications.readAll(customer1.getId());
            if (notifications.unreadCount(customer1.getId()) != 0) throw new AssertionError("readAll failed");

            // ---- J. SLA 与超时改约 ----
            LocalDateTime slotStart = LocalDateTime.now().plusHours(1);
            execute("INSERT INTO standard_time_slot(name,start_time,end_time,status) VALUES('9B-probe-slot',?,?,'ACTIVE')",
                    String.format("%02d:%02d:00", slotStart.getHour(), slotStart.getMinute()),
                    String.format("%02d:%02d:00", slotStart.plusHours(1).getHour(), slotStart.plusHours(1).getMinute()));
            long slaSlot = scalar("SELECT slot_id FROM standard_time_slot WHERE name='9B-probe-slot'");
            Date slaDate = Date.valueOf(slotStart.toLocalDate());
            engineers.addSchedule(engineerId, slaDate, slaSlot);
            long slaSched = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=?", engineerId, slaDate);
            long reqSla = customers.createRequest(customer1.getId(), deviceId, fault1, areaId, "9B-sla", "addr", "13977190002", slaDate, null);
            long apptSla = customers.book(customer1.getId(), reqSla, engineerId, slaSched, 0);
            int slaFirst = admins.runSla(adminId);
            if (slaFirst < 2) throw new AssertionError("sla first run expected >=2, got " + slaFirst);
            int slaSecond = admins.runSla(adminId);
            if (slaSecond != 0) throw new AssertionError("sla dedup failed, second run generated " + slaSecond);
            execute("UPDATE appointment SET status='PENDING_RESCHEDULE',updated_at=TIMESTAMPADD(HOUR,-25,CURRENT_TIMESTAMP) WHERE appointment_id=?", apptSla);
            int expired = admins.expireReschedules(adminId);
            if (expired != 1) throw new AssertionError("expireReschedules expected 1, got " + expired);
            assertText("EXPIRED", "SELECT status FROM appointment WHERE appointment_id=?", apptSla, "expire appointment");
            assertText("CANCELLED", "SELECT order_status FROM repair_order WHERE appointment_id=?", apptSla, "expire order");

            cleanupProbeData();
            System.out.println("STAGE9_MYSQL_TX_OK genKey=ok bookRollback=ok rescheduleRollback=ok adminReviewRollback=ok whReviewRollback=ok whIssueRollback=ok completeRollback=ok complete=ok inventoryConcurrency=ok bookingConcurrency=ok timezone=ok boolean=ok sla=ok expire=ok");
        } finally {
            if (direct != null && !direct.isClosed()) direct.close();
        }
    }

    private static void cleanupProbeData() throws Exception {
        execute("UPDATE part_inventory SET total_quantity=20,available_quantity=20,locked_quantity=0,issued_quantity=0 WHERE part_id=1");
        execute("UPDATE part_inventory SET total_quantity=8,available_quantity=8,locked_quantity=0,issued_quantity=0 WHERE part_id=2");
        execute("UPDATE part_inventory SET total_quantity=15,available_quantity=15,locked_quantity=0,issued_quantity=0 WHERE part_id=3");
        execute("DELETE FROM inventory_flow WHERE part_request_id IN (SELECT part_request_id FROM part_request WHERE reason LIKE '9B-probe%' OR reason LIKE '9B-conc%')");
        execute("DELETE FROM operation_log WHERE business_type='PART_REQUEST' AND business_id IN (SELECT part_request_id FROM part_request WHERE reason LIKE '9B-probe%' OR reason LIKE '9B-conc%')");
        execute("DELETE FROM notification WHERE related_business_type='PART_REQUEST' AND related_business_id IN (SELECT part_request_id FROM part_request WHERE reason LIKE '9B-probe%' OR reason LIKE '9B-conc%')");
        execute("DELETE FROM part_request_item WHERE part_request_id IN (SELECT part_request_id FROM part_request WHERE reason LIKE '9B-probe%' OR reason LIKE '9B-conc%')");
        execute("DELETE FROM part_request WHERE reason LIKE '9B-probe%' OR reason LIKE '9B-conc%'");
        execute("DELETE FROM rework WHERE order_id IN (SELECT o.order_id FROM repair_order o JOIN appointment a ON a.appointment_id=o.appointment_id JOIN repair_request r ON r.repair_request_id=a.request_id WHERE r.fault_description LIKE '9B-probe%' OR r.fault_description LIKE '9B-conc%' OR r.fault_description LIKE '9B-sla%')");
        execute("DELETE FROM review WHERE order_id IN (SELECT o.order_id FROM repair_order o JOIN appointment a ON a.appointment_id=o.appointment_id JOIN repair_request r ON r.repair_request_id=a.request_id WHERE r.fault_description LIKE '9B-probe%' OR r.fault_description LIKE '9B-conc%' OR r.fault_description LIKE '9B-sla%')");
        execute("DELETE FROM acceptance WHERE order_id IN (SELECT o.order_id FROM repair_order o JOIN appointment a ON a.appointment_id=o.appointment_id JOIN repair_request r ON r.repair_request_id=a.request_id WHERE r.fault_description LIKE '9B-probe%' OR r.fault_description LIKE '9B-conc%' OR r.fault_description LIKE '9B-sla%')");
        execute("DELETE FROM repair_record WHERE order_id IN (SELECT o.order_id FROM repair_order o JOIN appointment a ON a.appointment_id=o.appointment_id JOIN repair_request r ON r.repair_request_id=a.request_id WHERE r.fault_description LIKE '9B-probe%' OR r.fault_description LIKE '9B-conc%' OR r.fault_description LIKE '9B-sla%')");
        execute("DELETE FROM order_status_log WHERE order_id IN (SELECT o.order_id FROM repair_order o JOIN appointment a ON a.appointment_id=o.appointment_id JOIN repair_request r ON r.repair_request_id=a.request_id WHERE r.fault_description LIKE '9B-probe%' OR r.fault_description LIKE '9B-conc%' OR r.fault_description LIKE '9B-sla%')");
        execute("DELETE FROM repair_order WHERE appointment_id IN (SELECT a.appointment_id FROM appointment a JOIN repair_request r ON r.repair_request_id=a.request_id WHERE r.fault_description LIKE '9B-probe%' OR r.fault_description LIKE '9B-conc%' OR r.fault_description LIKE '9B-sla%')");
        execute("DELETE FROM appointment_change WHERE appointment_id IN (SELECT a.appointment_id FROM appointment a JOIN repair_request r ON r.repair_request_id=a.request_id WHERE r.fault_description LIKE '9B-probe%' OR r.fault_description LIKE '9B-conc%' OR r.fault_description LIKE '9B-sla%')");
        execute("DELETE FROM appointment WHERE request_id IN (SELECT repair_request_id FROM repair_request WHERE fault_description LIKE '9B-probe%' OR fault_description LIKE '9B-conc%' OR fault_description LIKE '9B-sla%')");
        execute("DELETE FROM repair_request WHERE fault_description LIKE '9B-probe%' OR fault_description LIKE '9B-conc%' OR fault_description LIKE '9B-sla%'");
        execute("DELETE FROM engineer_schedule WHERE service_date >= CURRENT_DATE AND engineer_id IN (SELECT user_id FROM system_user WHERE phone LIKE '1397719%')");
        execute("DELETE FROM engineer_application_skill WHERE application_id IN (SELECT application_id FROM engineer_application WHERE phone LIKE '1397719%')");
        execute("DELETE FROM engineer_application WHERE phone LIKE '1397719%'");
        execute("DELETE FROM review WHERE customer_id IN (SELECT user_id FROM system_user WHERE phone LIKE '1397719%')");
        execute("DELETE FROM acceptance WHERE customer_id IN (SELECT user_id FROM system_user WHERE phone LIKE '1397719%')");
        execute("DELETE FROM rework WHERE order_id IN (SELECT order_id FROM repair_order WHERE customer_id IN (SELECT user_id FROM system_user WHERE phone LIKE '1397719%'))");
        execute("DELETE FROM engineer_skill WHERE engineer_id IN (SELECT user_id FROM system_user WHERE phone LIKE '1397719%')");
        execute("DELETE FROM engineer_service_area WHERE engineer_id IN (SELECT user_id FROM system_user WHERE phone LIKE '1397719%')");
        execute("DELETE FROM engineer_profile WHERE engineer_id IN (SELECT user_id FROM system_user WHERE phone LIKE '1397719%')");
        execute("DELETE FROM customer_profile WHERE customer_id IN (SELECT user_id FROM system_user WHERE phone LIKE '1397719%')");
        execute("DELETE FROM operation_log WHERE user_id IN (SELECT user_id FROM system_user WHERE phone LIKE '1397719%')");
        execute("DELETE FROM notification WHERE receiver_id IN (SELECT user_id FROM system_user WHERE phone LIKE '1397719%') OR notification_type='9B_PROBE'");
        execute("DELETE FROM system_user WHERE phone LIKE '1397719%'");
        execute("DELETE FROM standard_time_slot WHERE name='9B-probe-slot'");
    }

    private static Connection open() throws Exception {
        if (direct == null || direct.isClosed()) direct = DriverManager.getConnection(url, user, password);
        return direct;
    }

    private interface Work { void run() throws Exception; }
    private static void expectFailure(Work work) throws Exception { boolean failed=false; try { work.run(); } catch (Exception expected) { failed=true; } if(!failed) throw new AssertionError("Expected failure did not occur"); }
    private static void assertCount(long expected, String sql, Object value, String label) throws Exception { long actual=scalar(sql,value); if(actual!=expected)throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static void assertCount(long expected, String sql, Object[] values, String label) throws Exception { long actual=scalar(sql,values); if(actual!=expected)throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static void assertCount(long expected, String sql, String label) throws Exception { long actual=scalar(sql); if(actual!=expected)throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static void assertText(String expected, String sql, Object value, String label) throws Exception { String actual=text(sql,value); if(!expected.equals(actual))throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static void assertText(String expected, String sql, String label) throws Exception { String actual=text(sql); if(!expected.equals(actual))throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static long scalar(String sql, Object... args) throws Exception { try(Connection c=open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new AssertionError("No row: "+sql);return rs.getLong(1);}} }
    private static String text(String sql, Object... args) throws Exception { try(Connection c=open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new AssertionError("No row: "+sql);return rs.getString(1);}} }
    private static void execute(String sql, Object... args) throws Exception { try(Connection c=open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);ps.executeUpdate();} }
    private static void bind(PreparedStatement ps,Object...args)throws Exception{for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);}
}
