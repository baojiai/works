import com.course.aftersales.model.SessionUser;
import com.course.aftersales.repository.Database;
import com.course.aftersales.service.AuthService;
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
import java.sql.Statement;
import java.time.LocalDate;

public class Stage7EngineerTransactionProbe {
    public static void main(String[] args) throws Exception {
        String base = args[0];
        Files.createDirectories(Paths.get(base, "data"));
        System.setProperty("catalina.base", base);
        Database.initialize();

        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml")) {
            AuthService auth = context.getBean(AuthService.class);
            EngineerApplicationService applications = context.getBean(EngineerApplicationService.class);
            EngineerService engineers = context.getBean(EngineerService.class);
            if (!AopUtils.isAopProxy(applications) || !AopUtils.isAopProxy(engineers)) throw new AssertionError("Engineer services are not transactional proxies");

            long adminId = scalar("SELECT user_id FROM system_user WHERE username='admin'");
            long areaId = scalar("SELECT MIN(service_area_id) FROM service_area WHERE status='ACTIVE'");
            long fault1 = scalar("SELECT MIN(fault_type_id) FROM fault_type WHERE status='ACTIVE'");
            long fault2 = scalar("SELECT MIN(fault_type_id) FROM fault_type WHERE status='ACTIVE' AND fault_type_id<>?", fault1);

            SessionUser rollbackApplicant = auth.registerCustomer("13977100001", "submit-rollback", "123456", "123456");
            expectFailure(() -> applications.submit(rollbackApplicant.getId(), "submit-rollback", "ID-RB", "13977100001", areaId, 1, "", "skill", "material", new String[]{String.valueOf(fault1), "999999"}));
            assertCount(0, "SELECT COUNT(*) FROM engineer_application WHERE user_id=?", rollbackApplicant.getId(), "application submit rollback");

            SessionUser applicant = auth.registerCustomer("13977100002", "engineer-user", "123456", "123456");
            applications.submit(applicant.getId(), "engineer-user", "ID-OK", "13977100002", areaId, 3, "CERT", "original-bio", "material", new String[]{String.valueOf(fault1)});
            long applicationId = scalar("SELECT application_id FROM engineer_application WHERE user_id=?", applicant.getId());

            execute("ALTER TABLE notification ADD CONSTRAINT ck_stage7_review_title CHECK(title <> '工程师认证已通过')");
            expectFailure(() -> applications.review(adminId, applicationId, true, "approve"));
            assertText("PENDING", "SELECT status FROM engineer_application WHERE application_id=?", applicationId, "review application rollback");
            assertText("CUSTOMER", "SELECT role_type FROM system_user WHERE user_id=?", applicant.getId(), "review user rollback");
            assertCount(0, "SELECT COUNT(*) FROM engineer_profile WHERE engineer_id=?", applicant.getId(), "review profile rollback");
            execute("ALTER TABLE notification DROP CONSTRAINT ck_stage7_review_title");
            applications.review(adminId, applicationId, true, "approve");
            assertText("APPROVED", "SELECT status FROM engineer_application WHERE application_id=?", applicationId, "review success");
            assertText("ENGINEER", "SELECT role_type FROM system_user WHERE user_id=?", applicant.getId(), "engineer role");

            long engineerId = applicant.getId();
            engineers.updateProfile(engineerId, "13977100003", "updated-bio", new String[]{String.valueOf(fault1), String.valueOf(fault2)}, new String[]{String.valueOf(areaId)});
            assertText("updated-bio", "SELECT bio FROM engineer_profile WHERE engineer_id=?", engineerId, "profile update");
            expectFailure(() -> engineers.updateProfile(engineerId, "13977100004", "must-rollback", new String[]{String.valueOf(fault1), "999999"}, new String[]{String.valueOf(areaId)}));
            assertText("13977100003", "SELECT phone FROM system_user WHERE user_id=?", engineerId, "profile phone rollback");
            assertText("updated-bio", "SELECT bio FROM engineer_profile WHERE engineer_id=?", engineerId, "profile bio rollback");
            assertCount(2, "SELECT COUNT(*) FROM engineer_skill WHERE engineer_id=?", engineerId, "profile skills rollback");

            Date scheduleDate = Date.valueOf(LocalDate.now().plusDays(5));
            long slot1 = scalar("SELECT MIN(slot_id) FROM standard_time_slot WHERE status='ACTIVE'");
            engineers.addSchedule(engineerId, scheduleDate, slot1);
            expectFailure(() -> engineers.addSchedule(engineerId, scheduleDate, slot1));
            assertCount(1, "SELECT COUNT(*) FROM engineer_schedule WHERE engineer_id=? AND service_date=DATEADD('DAY',5,CURRENT_DATE)", engineerId, "duplicate schedule rollback");
            assertCount(1, "SELECT COUNT(*) FROM operation_log WHERE user_id=? AND operation_type='CREATE_SCHEDULE'", engineerId, "duplicate schedule log rollback");
            long firstSchedule = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=DATEADD('DAY',5,CURRENT_DATE)", engineerId);
            engineers.closeSchedule(engineerId, firstSchedule);
            assertText("CLOSED", "SELECT status FROM engineer_schedule WHERE schedule_id=?", firstSchedule, "schedule close");

            SessionUser customer = auth.registerCustomer("13977100005", "order-customer", "123456", "123456");
            long deviceId = scalar("SELECT MIN(device_type_id) FROM device_type WHERE status='ACTIVE'");
            long requestId = insert("INSERT INTO repair_request(customer_id,device_type_id,fault_type_id,service_area_id,fault_description,service_address,contact_phone,expected_date,status) VALUES(?,?,?,?,?,?,?,DATEADD('DAY',6,CURRENT_DATE),'OPEN')",
                    customer.getId(), deviceId, fault1, areaId, "fault", "address", "13977100005");
            long slot2 = scalar("SELECT MIN(slot_id) FROM standard_time_slot WHERE status='ACTIVE' AND slot_id<>?", slot1);
            Date orderDate = Date.valueOf(LocalDate.now().plusDays(6));
            engineers.addSchedule(engineerId, orderDate, slot2);
            long bookedSchedule = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=DATEADD('DAY',6,CURRENT_DATE)", engineerId);
            execute("UPDATE engineer_schedule SET status='BOOKED' WHERE schedule_id=?", bookedSchedule);
            long appointmentId = insert("INSERT INTO appointment(appointment_no,request_id,customer_id,engineer_id,schedule_id,status) VALUES(?,?,?,?,?,'BOOKED')",
                    "AP-TX-1", requestId, customer.getId(), engineerId, bookedSchedule);
            long orderId = insert("INSERT INTO repair_order(order_no,appointment_id,customer_id,engineer_id,order_status) VALUES(?,?,?,?,'PENDING_VISIT')",
                    "RO-TX-1", appointmentId, customer.getId(), engineerId);
            engineers.transition(engineerId, orderId, "START");
            assertText("REPAIRING", "SELECT order_status FROM repair_order WHERE order_id=?", orderId, "order start");

            execute("ALTER TABLE order_status_log ADD CONSTRAINT ck_stage7_wait_parts CHECK(new_status <> 'WAITING_PARTS')");
            expectFailure(() -> engineers.transition(engineerId, orderId, "WAIT_PARTS"));
            assertText("REPAIRING", "SELECT order_status FROM repair_order WHERE order_id=?", orderId, "order transition rollback");
            execute("ALTER TABLE order_status_log DROP CONSTRAINT ck_stage7_wait_parts");

            execute("ALTER TABLE operation_log ADD CONSTRAINT ck_stage7_record_log CHECK(operation_type <> 'ADD_REPAIR_RECORD')");
            expectFailure(() -> engineers.saveRecord(engineerId, orderId, "diagnosis", "action", 1.5, "rollback"));
            assertCount(0, "SELECT COUNT(*) FROM repair_record WHERE order_id=?", orderId, "repair record rollback");
            execute("ALTER TABLE operation_log DROP CONSTRAINT ck_stage7_record_log");
            engineers.saveRecord(engineerId, orderId, "diagnosis", "action", 1.5, "success");
            assertCount(1, "SELECT COUNT(*) FROM repair_record WHERE order_id=?", orderId, "repair record save");

            if (applications.formData(engineerId).isEmpty() || applications.applications().isEmpty() || engineers.profileData(engineerId).isEmpty() || engineers.scheduleData(engineerId).isEmpty()) {
                throw new AssertionError("Read-only service methods failed");
            }
            System.out.println("STAGE7_ENGINEER_TX_OK submit=ok submitRollback=ok review=ok reviewRollback=ok profile=ok profileRollback=ok schedule=ok duplicateRollback=ok close=ok orderStart=ok transitionRollback=ok record=ok recordRollback=ok");
        }
    }

    private interface Work { void run() throws Exception; }
    private static void expectFailure(Work work) throws Exception { boolean failed=false; try { work.run(); } catch (Exception expected) { failed=true; } if(!failed) throw new AssertionError("Expected failure did not occur"); }
    private static void assertCount(long expected, String sql, Object value, String label) throws Exception { long actual=scalar(sql,value); if(actual!=expected)throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static void assertText(String expected, String sql, Object value, String label) throws Exception { String actual=text(sql,value); if(!expected.equals(actual))throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static long scalar(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new AssertionError("No row: "+sql);return rs.getLong(1);}} }
    private static String text(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new AssertionError("No row: "+sql);return rs.getString(1);}} }
    private static void execute(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);ps.executeUpdate();} }
    private static long insert(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){bind(ps,args);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new AssertionError("No generated key");return rs.getLong(1);}} }
    private static void bind(PreparedStatement ps,Object...args)throws Exception{for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);}
}
