import com.course.aftersales.model.SessionUser;
import com.course.aftersales.repository.Database;
import com.course.aftersales.service.AdminService;
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
import java.time.LocalDateTime;

public class Stage7AdminTransactionProbe {
    public static void main(String[] args) throws Exception {
        String base = args[0];
        Files.createDirectories(Paths.get(base, "data"));
        System.setProperty("catalina.base", base);
        Database.initialize();

        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml")) {
            AuthService auth = context.getBean(AuthService.class);
            AdminService admins = context.getBean(AdminService.class);
            CustomerService customers = context.getBean(CustomerService.class);
            EngineerApplicationService applications = context.getBean(EngineerApplicationService.class);
            EngineerService engineers = context.getBean(EngineerService.class);
            if (!AopUtils.isAopProxy(admins)) throw new AssertionError("AdminService is not a transactional proxy");

            long adminId = scalar("SELECT user_id FROM system_user WHERE username='admin'");
            long areaId = scalar("SELECT MIN(service_area_id) FROM service_area WHERE status='ACTIVE'");
            long deviceId = scalar("SELECT MIN(device_type_id) FROM device_type WHERE status='ACTIVE'");
            long fault1 = scalar("SELECT MIN(fault_type_id) FROM fault_type WHERE status='ACTIVE' AND device_type_id=?", deviceId);
            long slot1 = scalar("SELECT MIN(slot_id) FROM standard_time_slot WHERE status='ACTIVE'");

            // ---- Test 2: review application (rollback first, then success) ----
            SessionUser applicant = auth.registerCustomer("13977130001", "tx-admin-eng", "123456", "123456");
            applications.submit(applicant.getId(), "tx-admin-eng", "ID-TX-AE", "13977130001", areaId, 3, "CERT", "bio", "material", new String[]{String.valueOf(fault1)});
            long applicationId = scalar("SELECT application_id FROM engineer_application WHERE user_id=?", applicant.getId());
            execute("ALTER TABLE notification ADD CONSTRAINT ck_stage7_admin_review CHECK(title <> '工程师认证已通过')");
            expectFailure(() -> admins.reviewApplication(adminId, applicationId, true, "approve"));
            assertText("PENDING", "SELECT status FROM engineer_application WHERE application_id=?", applicationId, "admin review rollback application");
            assertText("CUSTOMER", "SELECT role_type FROM system_user WHERE user_id=?", applicant.getId(), "admin review rollback role");
            assertCount(0, "SELECT COUNT(*) FROM engineer_profile WHERE engineer_id=?", applicant.getId(), "admin review rollback profile");
            assertCount(0, "SELECT COUNT(*) FROM engineer_skill WHERE engineer_id=?", applicant.getId(), "admin review rollback skill");
            assertCount(0, "SELECT COUNT(*) FROM engineer_service_area WHERE engineer_id=?", applicant.getId(), "admin review rollback area");
            execute("ALTER TABLE notification DROP CONSTRAINT ck_stage7_admin_review");
            admins.reviewApplication(adminId, applicationId, true, "approve");
            assertText("APPROVED", "SELECT status FROM engineer_application WHERE application_id=?", applicationId, "admin review success application");
            assertText("ENGINEER", "SELECT role_type FROM system_user WHERE user_id=?", applicant.getId(), "admin review success role");
            long engineerId = applicant.getId();

            // ---- Test 1: user status (rollback first, then success) ----
            SessionUser customer1 = auth.registerCustomer("13977130002", "tx-admin-cust", "123456", "123456");
            if (auth.login("13977130002", "123456") == null) throw new AssertionError("customer login before disable");
            execute("ALTER TABLE operation_log ADD CONSTRAINT ck_stage7_admin_user_status CHECK(operation_type <> 'SET_USER_STATUS')");
            expectFailure(() -> admins.setUserStatus(adminId, customer1.getId(), "DISABLED"));
            assertText("ACTIVE", "SELECT status FROM system_user WHERE user_id=?", customer1.getId(), "user status rollback");
            if (auth.login("13977130002", "123456") == null) throw new AssertionError("rollback left user disabled");
            execute("ALTER TABLE operation_log DROP CONSTRAINT ck_stage7_admin_user_status");
            admins.setUserStatus(adminId, customer1.getId(), "DISABLED");
            assertText("DISABLED", "SELECT status FROM system_user WHERE user_id=?", customer1.getId(), "disable user");
            if (auth.login("13977130002", "123456") != null) throw new AssertionError("disabled user still logs in");
            admins.setUserStatus(adminId, customer1.getId(), "ACTIVE");
            assertText("ACTIVE", "SELECT status FROM system_user WHERE user_id=?", customer1.getId(), "enable user");
            if (auth.login("13977130002", "123456") == null) throw new AssertionError("enabled user cannot log in");

            // ---- Test 3: system config (rollback first, then update/insert) ----
            execute("ALTER TABLE operation_log ADD CONSTRAINT ck_stage7_admin_config CHECK(operation_type <> 'UPDATE_CONFIG')");
            expectFailure(() -> admins.updateConfig(adminId, "CANCEL_HOURS", "9"));
            assertText("2", "SELECT config_value FROM system_config WHERE config_key='CANCEL_HOURS'", "config rollback existing key");
            expectFailure(() -> admins.updateConfig(adminId, "STAGE7_TX_ROLLBACK", "x"));
            assertCount(0, "SELECT COUNT(*) FROM system_config WHERE config_key='STAGE7_TX_ROLLBACK'", "config rollback no new key");
            execute("ALTER TABLE operation_log DROP CONSTRAINT ck_stage7_admin_config");
            admins.updateConfig(adminId, "CANCEL_HOURS", "3");
            assertText("3", "SELECT config_value FROM system_config WHERE config_key='CANCEL_HOURS'", "config update");
            admins.updateConfig(adminId, "STAGE7_TX_KEY", "v1");
            assertText("v1", "SELECT config_value FROM system_config WHERE config_key='STAGE7_TX_KEY'", "config insert");
            admins.updateConfig(adminId, "CANCEL_HOURS", "2");

            // ---- Test 4: basic data (device/fault/area/slot) ----
            admins.addBasic(adminId, "device", "TX-设备", 0, "", "");
            assertCount(1, "SELECT COUNT(*) FROM device_type WHERE name='TX-设备'", "basic device");
            admins.addBasic(adminId, "fault", "TX-故障", deviceId, "", "");
            assertCount(1, "SELECT COUNT(*) FROM fault_type WHERE name='TX-故障' AND device_type_id=?", deviceId, "basic fault");
            admins.addBasic(adminId, "area", "TX-区域", 0, "", "");
            assertCount(1, "SELECT COUNT(*) FROM service_area WHERE name='TX-区域'", "basic area");
            LocalDateTime slotStart = LocalDateTime.now().plusHours(1);
            String startText = String.format("%02d:%02d", slotStart.getHour(), slotStart.getMinute());
            LocalDateTime slotEnd = slotStart.plusHours(1);
            String endText = String.format("%02d:%02d", slotEnd.getHour(), slotEnd.getMinute());
            admins.addBasic(adminId, "slot", "TX-SLA-时段", 0, startText, endText);
            long slaSlotId = scalar("SELECT slot_id FROM standard_time_slot WHERE name='TX-SLA-时段'");
            assertCount(1, "SELECT COUNT(*) FROM operation_log WHERE operation_type='ADD_BASIC_DATA' AND business_type='SLOT'", "basic slot log");

            // ---- Test 5: SLA check (rollback first, success, dedup) ----
            Date slaDate = Date.valueOf(slotStart.toLocalDate());
            engineers.addSchedule(engineerId, slaDate, slaSlotId);
            long slaSchedule = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=?", engineerId, slaDate);
            long requestId1 = customers.createRequest(customer1.getId(), deviceId, fault1, areaId, "SLA报修", "软件园A座", "13977130002", slaDate, null);
            long slaAppointment = customers.book(customer1.getId(), requestId1, engineerId, slaSchedule, 0);
            long slaOrder = scalar("SELECT order_id FROM repair_order WHERE appointment_id=?", slaAppointment);
            execute("UPDATE repair_order SET order_status='REPAIRING',created_at=TIMESTAMPADD('HOUR',-50,CURRENT_TIMESTAMP) WHERE order_id=?", slaOrder);
            execute("ALTER TABLE operation_log ADD CONSTRAINT ck_stage7_admin_sla CHECK(operation_type <> 'RUN_SLA')");
            expectFailure(() -> admins.runSla(adminId));
            assertCount(0, "SELECT COUNT(*) FROM notification WHERE notification_type='APPOINTMENT_REMINDER'", "sla rollback reminder");
            assertCount(0, "SELECT COUNT(*) FROM notification WHERE notification_type='ORDER_OVERDUE'", "sla rollback overdue");
            assertCount(0, "SELECT COUNT(*) FROM operation_log WHERE operation_type='RUN_SLA'", "sla rollback log");
            execute("ALTER TABLE operation_log DROP CONSTRAINT ck_stage7_admin_sla");
            int slaCount = admins.runSla(adminId);
            assertCount(2, "SELECT COUNT(*) FROM notification WHERE notification_type='APPOINTMENT_REMINDER' AND related_business_id=?", slaAppointment, "sla reminder notifications");
            assertCount(1, "SELECT COUNT(*) FROM notification WHERE notification_type='ORDER_OVERDUE' AND related_business_id=?", slaOrder, "sla overdue notification");
            if (slaCount < 3) throw new AssertionError("sla generated count too low: " + slaCount);
            int slaSecond = admins.runSla(adminId);
            if (slaSecond != 0) throw new AssertionError("sla dedup failed: " + slaSecond);
            assertCount(2, "SELECT COUNT(*) FROM notification WHERE notification_type='APPOINTMENT_REMINDER' AND related_business_id=?", slaAppointment, "sla dedup reminder unchanged");
            assertCount(1, "SELECT COUNT(*) FROM notification WHERE notification_type='ORDER_OVERDUE' AND related_business_id=?", slaOrder, "sla dedup overdue unchanged");

            // ---- Test 6: expire reschedules (rollback first, then success) ----
            Date d2 = Date.valueOf(LocalDate.now().plusDays(2));
            engineers.addSchedule(engineerId, d2, slot1);
            long expireSchedule = scalar("SELECT schedule_id FROM engineer_schedule WHERE engineer_id=? AND service_date=?", engineerId, d2);
            long requestId2 = customers.createRequest(customer1.getId(), deviceId, fault1, areaId, "超时报修", "软件园A座", "13977130002", d2, null);
            long expireAppointment = customers.book(customer1.getId(), requestId2, engineerId, expireSchedule, 0);
            long expireOrder = scalar("SELECT order_id FROM repair_order WHERE appointment_id=?", expireAppointment);
            execute("UPDATE appointment SET status='PENDING_RESCHEDULE',updated_at=TIMESTAMPADD('HOUR',-25,CURRENT_TIMESTAMP) WHERE appointment_id=?", expireAppointment);
            execute("ALTER TABLE operation_log ADD CONSTRAINT ck_stage7_admin_expire CHECK(operation_type <> 'EXPIRE_RESCHEDULES')");
            expectFailure(() -> admins.expireReschedules(adminId));
            assertText("PENDING_RESCHEDULE", "SELECT status FROM appointment WHERE appointment_id=?", expireAppointment, "expire rollback appointment");
            assertText("PENDING_VISIT", "SELECT order_status FROM repair_order WHERE order_id=?", expireOrder, "expire rollback order");
            assertCount(0, "SELECT COUNT(*) FROM appointment_change WHERE appointment_id=?", expireAppointment, "expire rollback change");
            assertCount(0, "SELECT COUNT(*) FROM notification WHERE notification_type='RESCHEDULE_EXPIRED' AND related_business_id=?", expireAppointment, "expire rollback notification");
            assertCount(0, "SELECT COUNT(*) FROM operation_log WHERE operation_type='EXPIRE_RESCHEDULES'", "expire rollback log");
            execute("ALTER TABLE operation_log DROP CONSTRAINT ck_stage7_admin_expire");
            int expireCount = admins.expireReschedules(adminId);
            if (expireCount != 1) throw new AssertionError("expire count expected 1, actual " + expireCount);
            assertText("EXPIRED", "SELECT status FROM appointment WHERE appointment_id=?", expireAppointment, "expire appointment");
            assertText("CANCELLED", "SELECT order_status FROM repair_order WHERE order_id=?", expireOrder, "expire order");
            assertCount(1, "SELECT COUNT(*) FROM appointment_change WHERE appointment_id=?", expireAppointment, "expire change");
            assertCount(1, "SELECT COUNT(*) FROM notification WHERE notification_type='RESCHEDULE_EXPIRED' AND related_business_id=?", expireAppointment, "expire notification");
            assertCount(1, "SELECT COUNT(*) FROM operation_log WHERE operation_type='EXPIRE_RESCHEDULES'", "expire log");

            // ---- Read-only data() ----
            if (admins.data().isEmpty()) throw new AssertionError("admin data empty");

            System.out.println("STAGE7_ADMIN_TX_OK reviewRollback=ok review=ok disable=ok enable=ok userStatusRollback=ok configUpdate=ok configInsert=ok configRollback=ok basic=ok slaRollback=ok sla=ok slaDedup=ok expireRollback=ok expire=ok data=ok");
        }
    }

    private interface Work { void run() throws Exception; }
    private static void expectFailure(Work work) throws Exception { boolean failed=false; try { work.run(); } catch (Exception expected) { failed=true; } if(!failed) throw new AssertionError("Expected failure did not occur"); }
    private static void assertCount(long expected, String sql, Object value, String label) throws Exception { long actual=scalar(sql,value); if(actual!=expected)throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static void assertCount(long expected, String sql, String label) throws Exception { long actual=scalar(sql); if(actual!=expected)throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static void assertText(String expected, String sql, Object value, String label) throws Exception { String actual=text(sql,value); if(!expected.equals(actual))throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static void assertText(String expected, String sql, String label) throws Exception { String actual=text(sql); if(!expected.equals(actual))throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static long scalar(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new AssertionError("No row: "+sql);return rs.getLong(1);}} }
    private static String text(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new AssertionError("No row: "+sql);return rs.getString(1);}} }
    private static void execute(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);ps.executeUpdate();} }
    private static void bind(PreparedStatement ps,Object...args)throws Exception{for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);}
}
