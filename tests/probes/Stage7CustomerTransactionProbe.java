import com.course.aftersales.model.SessionUser;
import com.course.aftersales.repository.Database;
import com.course.aftersales.service.AuthService;
import com.course.aftersales.service.CustomerService;
import com.course.aftersales.service.EngineerApplicationService;
import com.course.aftersales.service.EngineerService;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class Stage7CustomerTransactionProbe {
    public static void main(String[] args) throws Exception {
        String base = args[0];
        Files.createDirectories(Paths.get(base, "data"));
        System.setProperty("catalina.base", base);
        Database.initialize();

        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml")) {
            AuthService auth = context.getBean(AuthService.class);
            CustomerService customers = context.getBean(CustomerService.class);
            EngineerApplicationService applications = context.getBean(EngineerApplicationService.class);
            EngineerService engineers = context.getBean(EngineerService.class);
            if (!AopUtils.isAopProxy(customers)) throw new AssertionError("CustomerService is not a transactional proxy");

            long adminId = scalar("SELECT user_id FROM system_user WHERE username='admin'");
            long areaId = scalar("SELECT MIN(service_area_id) FROM service_area WHERE status='ACTIVE'");
            long deviceId = scalar("SELECT MIN(device_type_id) FROM device_type WHERE status='ACTIVE'");
            long fault1 = scalar("SELECT MIN(fault_type_id) FROM fault_type WHERE status='ACTIVE' AND device_type_id=?", deviceId);
            long slot1 = scalar("SELECT MIN(slot_id) FROM standard_time_slot WHERE status='ACTIVE'");
            long slot2 = scalar("SELECT MIN(slot_id) FROM standard_time_slot WHERE status='ACTIVE' AND slot_id<>?", slot1);
            Date d5 = Date.valueOf(LocalDate.now().plusDays(5));
            Date d6 = Date.valueOf(LocalDate.now().plusDays(6));

            SessionUser applicant = auth.registerCustomer("13977120001", "tx-engineer", "123456", "123456");
            applications.submit(applicant.getId(), "tx-engineer", "ID-TX-ENG", "13977120001", areaId, 3, "CERT", "bio", "material", new String[]{String.valueOf(fault1)});
            long applicationId = scalar("SELECT application_id FROM engineer_application WHERE user_id=?", applicant.getId());
            applications.review(adminId, applicationId, true, "approve");
            long engineerId = applicant.getId();
            engineers.addSchedule(engineerId, d5, slot1);
            long schedule1 = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=DATEADD('DAY',5,CURRENT_DATE)", engineerId);

            SessionUser customer1 = auth.registerCustomer("13977120002", "tx-customer", "123456", "123456");
            long requestId1 = customers.createRequest(customer1.getId(), deviceId, fault1, areaId, "屏幕故障", "软件园A座", "13977120002", d5, null);
            assertText("OPEN", "SELECT status FROM repair_request WHERE repair_request_id=?", requestId1, "create request");

            execute("ALTER TABLE operation_log ADD CONSTRAINT ck_stage7_book_log CHECK(operation_type <> 'CREATE_APPOINTMENT')");
            expectFailure(() -> customers.book(customer1.getId(), requestId1, engineerId, schedule1, 0));
            assertCount(0, "SELECT COUNT(*) FROM appointment WHERE request_id=?", requestId1, "book rollback appointment");
            assertCount(0, "SELECT COUNT(*) FROM repair_order WHERE appointment_id IN (SELECT appointment_id FROM appointment WHERE request_id=?)", requestId1, "book rollback order");
            assertText("AVAILABLE", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedule1, "book rollback schedule");
            assertText("OPEN", "SELECT status FROM repair_request WHERE repair_request_id=?", requestId1, "book rollback request");
            assertCount(0, "SELECT COUNT(*) FROM notification WHERE notification_type='NEW_APPOINTMENT' AND receiver_id=?", engineerId, "book rollback notification");
            assertCount(0, "SELECT COUNT(*) FROM operation_log WHERE operation_type='CREATE_APPOINTMENT' AND user_id=?", customer1.getId(), "book rollback log");
            execute("ALTER TABLE operation_log DROP CONSTRAINT ck_stage7_book_log");

            long appointmentId1 = customers.book(customer1.getId(), requestId1, engineerId, schedule1, 0);
            assertText("BOOKED", "SELECT status FROM appointment WHERE appointment_id=?", appointmentId1, "book appointment");
            assertText("OCCUPIED", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedule1, "book schedule occupied");
            assertText("BOOKED", "SELECT status FROM repair_request WHERE repair_request_id=?", requestId1, "book request booked");
            long orderId1 = scalar("SELECT order_id FROM repair_order WHERE appointment_id=?", appointmentId1);
            assertText("PENDING_VISIT", "SELECT order_status FROM repair_order WHERE order_id=?", orderId1, "book order");
            assertCount(1, "SELECT COUNT(*) FROM notification WHERE receiver_id=? AND notification_type='NEW_APPOINTMENT'", engineerId, "book notification");
            assertCount(1, "SELECT COUNT(*) FROM operation_log WHERE operation_type='CREATE_APPOINTMENT' AND business_id=?", appointmentId1, "book log");

            engineers.addSchedule(engineerId, d6, slot2);
            long schedule2 = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=DATEADD('DAY',6,CURRENT_DATE)", engineerId);
            execute("ALTER TABLE order_status_log ADD CONSTRAINT ck_stage7_resch_log CHECK(reason <> '客户改约，原工单关闭')");
            expectFailure(() -> customers.book(customer1.getId(), requestId1, engineerId, schedule2, appointmentId1));
            assertText("BOOKED", "SELECT status FROM appointment WHERE appointment_id=?", appointmentId1, "reschedule rollback old appointment");
            assertText("OCCUPIED", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedule1, "reschedule rollback old schedule");
            assertText("PENDING_VISIT", "SELECT order_status FROM repair_order WHERE order_id=?", orderId1, "reschedule rollback old order");
            assertText("AVAILABLE", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedule2, "reschedule rollback new schedule");
            assertCount(1, "SELECT COUNT(*) FROM appointment WHERE request_id=?", requestId1, "reschedule rollback no new appointment");
            assertCount(0, "SELECT COUNT(*) FROM appointment_change WHERE appointment_id=?", appointmentId1, "reschedule rollback no change row");
            assertCount(1, "SELECT COUNT(*) FROM notification WHERE receiver_id=? AND notification_type='NEW_APPOINTMENT'", engineerId, "reschedule rollback no new notification");
            execute("ALTER TABLE order_status_log DROP CONSTRAINT ck_stage7_resch_log");

            long appointmentId2 = customers.book(customer1.getId(), requestId1, engineerId, schedule2, appointmentId1);
            assertText("RESCHEDULED", "SELECT status FROM appointment WHERE appointment_id=?", appointmentId1, "reschedule old appointment");
            assertText("AVAILABLE", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedule1, "reschedule old schedule released");
            assertText("CANCELLED", "SELECT order_status FROM repair_order WHERE order_id=?", orderId1, "reschedule old order cancelled");
            assertText("BOOKED", "SELECT status FROM appointment WHERE appointment_id=?", appointmentId2, "reschedule new appointment");
            assertText("OCCUPIED", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedule2, "reschedule new schedule occupied");
            long orderId2 = scalar("SELECT order_id FROM repair_order WHERE appointment_id=?", appointmentId2);
            assertText("PENDING_VISIT", "SELECT order_status FROM repair_order WHERE order_id=?", orderId2, "reschedule new order");
            assertCount(1, "SELECT COUNT(*) FROM appointment_change WHERE appointment_id=?", appointmentId1, "reschedule change row");

            customers.cancel(customer1.getId(), appointmentId2, "临时有事");
            assertText("CANCELLED", "SELECT status FROM appointment WHERE appointment_id=?", appointmentId2, "cancel appointment");
            assertText("AVAILABLE", "SELECT status FROM engineer_schedule WHERE schedule_id=?", schedule2, "cancel schedule released");
            assertText("CANCELLED", "SELECT order_status FROM repair_order WHERE order_id=?", orderId2, "cancel order");
            assertCount(1, "SELECT COUNT(*) FROM operation_log WHERE operation_type='CANCEL_APPOINTMENT' AND business_id=?", appointmentId2, "cancel log");

            long requestId2 = customers.createRequest(customer1.getId(), deviceId, fault1, areaId, "再次报修", "软件园A座", "13977120002", d6, null);
            long appointmentId3 = customers.book(customer1.getId(), requestId2, engineerId, schedule2, 0);
            long orderId3 = scalar("SELECT order_id FROM repair_order WHERE appointment_id=?", appointmentId3);
            engineers.transition(engineerId, orderId3, "START");
            engineers.saveRecord(engineerId, orderId3, "诊断", "更换部件", 1.0, "note");
            engineers.transition(engineerId, orderId3, "FINISH");
            assertText("PENDING_ACCEPTANCE", "SELECT order_status FROM repair_order WHERE order_id=?", orderId3, "order ready for acceptance");

            execute("ALTER TABLE operation_log ADD CONSTRAINT ck_stage7_accept_log CHECK(operation_type <> 'ACCEPT_ORDER')");
            expectFailure(() -> customers.accept(customer1.getId(), orderId3, true, "验收通过"));
            assertText("PENDING_ACCEPTANCE", "SELECT order_status FROM repair_order WHERE order_id=?", orderId3, "accept rollback order");
            assertCount(0, "SELECT COUNT(*) FROM acceptance WHERE order_id=?", orderId3, "accept rollback acceptance");
            assertText("BOOKED", "SELECT status FROM appointment WHERE appointment_id=?", appointmentId3, "accept rollback appointment");
            assertCount(0, "SELECT COUNT(*) FROM order_status_log WHERE order_id=? AND new_status='COMPLETED'", orderId3, "accept rollback status log");
            execute("ALTER TABLE operation_log DROP CONSTRAINT ck_stage7_accept_log");

            customers.accept(customer1.getId(), orderId3, true, "验收通过");
            assertText("COMPLETED", "SELECT order_status FROM repair_order WHERE order_id=?", orderId3, "accept order completed");
            assertText("PASSED", "SELECT result FROM acceptance WHERE order_id=?", orderId3, "accept acceptance");
            assertText("FULFILLED", "SELECT status FROM appointment WHERE appointment_id=?", appointmentId3, "accept appointment fulfilled");

            long reviewCountBefore = scalar("SELECT review_count FROM engineer_profile WHERE engineer_id=?", engineerId);
            execute("ALTER TABLE operation_log ADD CONSTRAINT ck_stage7_review_log CHECK(operation_type <> 'CREATE_REVIEW')");
            expectFailure(() -> customers.review(customer1.getId(), orderId3, 5, "服务很好"));
            assertCount(0, "SELECT COUNT(*) FROM review WHERE order_id=?", orderId3, "review rollback");
            assertCount(reviewCountBefore, "SELECT review_count FROM engineer_profile WHERE engineer_id=?", engineerId, "review rollback stats");
            execute("ALTER TABLE operation_log DROP CONSTRAINT ck_stage7_review_log");

            customers.review(customer1.getId(), orderId3, 5, "服务很好");
            assertCount(1, "SELECT COUNT(*) FROM review WHERE order_id=?", orderId3, "review saved");
            assertText("VALID", "SELECT status FROM review WHERE order_id=?", orderId3, "review status");
            assertCount(1, "SELECT COUNT(*) FROM operation_log WHERE operation_type='CREATE_REVIEW' AND business_id=?", orderId3, "review log");

            SessionUser customerUser = new SessionUser(customer1.getId(), "tx-customer", "tx-customer", "CUSTOMER");
            if (customers.formData().isEmpty()) throw new AssertionError("formData empty");
            if (customers.request(customer1.getId(), requestId1) == null) throw new AssertionError("request empty");
            if (customers.candidates(customer1.getId(), requestId1, "rating").isEmpty()) throw new AssertionError("candidates empty");
            if (customers.appointments(customerUser).isEmpty()) throw new AssertionError("appointments empty");
            if (customers.orders(customerUser).isEmpty()) throw new AssertionError("orders empty");
            if (customers.orderDetail(customerUser, orderId3) == null) throw new AssertionError("orderDetail empty");

            System.out.println("STAGE7_CUSTOMER_TX_OK createRequest=ok bookRollback=ok book=ok rescheduleRollback=ok reschedule=ok cancel=ok acceptRollback=ok accept=ok reviewRollback=ok review=ok readOnly=ok");
        }
    }

    private interface Work { void run() throws Exception; }
    private static void expectFailure(Work work) throws Exception { boolean failed=false; try { work.run(); } catch (Exception expected) { failed=true; } if(!failed) throw new AssertionError("Expected failure did not occur"); }
    private static void assertCount(long expected, String sql, Object value, String label) throws Exception { long actual=scalar(sql,value); if(actual!=expected)throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static void assertText(String expected, String sql, Object value, String label) throws Exception { String actual=text(sql,value); if(!expected.equals(actual))throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static long scalar(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new AssertionError("No row: "+sql);return rs.getLong(1);}} }
    private static String text(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new AssertionError("No row: "+sql);return rs.getString(1);}} }
    private static void execute(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);ps.executeUpdate();} }
    private static void bind(PreparedStatement ps,Object...args)throws Exception{for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);}
}
