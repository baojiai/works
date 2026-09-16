import com.course.aftersales.model.SessionUser;
import com.course.aftersales.repository.Database;
import com.course.aftersales.service.AuthService;
import com.course.aftersales.service.EngineerApplicationService;
import com.course.aftersales.service.WarehouseService;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

public class Stage7WarehouseTransactionProbe {
    public static void main(String[] args) throws Exception {
        String base = args[0];
        Files.createDirectories(Paths.get(base, "data"));
        System.setProperty("catalina.base", base);
        Database.initialize();

        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml")) {
            AuthService auth = context.getBean(AuthService.class);
            WarehouseService warehouse = context.getBean(WarehouseService.class);
            EngineerApplicationService applications = context.getBean(EngineerApplicationService.class);
            if (!AopUtils.isAopProxy(warehouse)) throw new AssertionError("WarehouseService is not a transactional proxy");

            long adminId = scalar("SELECT user_id FROM system_user WHERE username='admin'");
            long warehouseId = scalar("SELECT user_id FROM system_user WHERE username='warehouse'");
            long areaId = scalar("SELECT MIN(service_area_id) FROM service_area WHERE status='ACTIVE'");
            long deviceId = scalar("SELECT MIN(device_type_id) FROM device_type WHERE status='ACTIVE'");
            long fault1 = scalar("SELECT MIN(fault_type_id) FROM fault_type WHERE status='ACTIVE' AND device_type_id=?", deviceId);

            // engineer + order + request #1 (2 items)
            SessionUser applicant = auth.registerCustomer("13977140001", "tx-wh-eng", "123456", "123456");
            applications.submit(applicant.getId(), "tx-wh-eng", "ID-TX-WH", "13977140001", areaId, 3, "CERT", "bio", "material", new String[]{String.valueOf(fault1)});
            long applicationId = scalar("SELECT application_id FROM engineer_application WHERE user_id=?", applicant.getId());
            applications.review(adminId, applicationId, true, "approve");
            long engineerId = applicant.getId();

            SessionUser customer = auth.registerCustomer("13977140002", "tx-wh-cust", "123456", "123456");
            long slot1 = scalar("SELECT MIN(slot_id) FROM standard_time_slot WHERE status='ACTIVE'");
            long scheduleId = insert("INSERT INTO engineer_schedule(engineer_id,service_date,slot_id,status) VALUES(?,DATEADD('DAY',3,CURRENT_DATE),?,'AVAILABLE')", engineerId, slot1);
            long requestId = insert("INSERT INTO repair_request(customer_id,device_type_id,fault_type_id,service_area_id,fault_description,service_address,contact_phone,expected_date,status) VALUES(?,?,?,?,?,?,?,DATEADD('DAY',3,CURRENT_DATE),'OPEN')",
                    customer.getId(), deviceId, fault1, areaId, "fault", "address", "13977140002");
            long appointmentId = insert("INSERT INTO appointment(appointment_no,request_id,customer_id,engineer_id,schedule_id,status) VALUES(?,?,?,?,?,'BOOKED')",
                    "AP-TX-WH-1", requestId, customer.getId(), engineerId, scheduleId);
            long orderId = insert("INSERT INTO repair_order(order_no,appointment_id,customer_id,engineer_id,order_status) VALUES(?,?,?,?,'REPAIRING')",
                    "RO-TX-WH-1", appointmentId, customer.getId(), engineerId);
            long pr1 = insert("INSERT INTO part_request(request_no,order_id,engineer_id,status,reason) VALUES(?,?,?,'PENDING','回归配件申请')",
                    "PR-TX-WH-1", orderId, engineerId);
            long item1 = insert("INSERT INTO part_request_item(part_request_id,part_id,request_quantity) VALUES(?,1,2)", pr1);
            long item2 = insert("INSERT INTO part_request_item(part_request_id,part_id,request_quantity) VALUES(?,2,1)", pr1);
            assertInventory(1, 20, 20, 0, 0, "part1 initial");
            assertInventory(2, 8, 8, 0, 0, "part2 initial");

            // ---- review approve rollback: fail at second item LOCK flow ----
            execute("ALTER TABLE inventory_flow ADD CONSTRAINT ck_stage7_wh_review CHECK(flow_type <> 'LOCK' OR part_id <> 2)");
            expectFailure(() -> warehouse.review(warehouseId, pr1, true, "approve"));
            assertInventory(1, 20, 20, 0, 0, "review rollback part1");
            assertInventory(2, 8, 8, 0, 0, "review rollback part2");
            assertText("PENDING", "SELECT status FROM part_request WHERE part_request_id=?", pr1, "review rollback request");
            assertCount(0, "SELECT COUNT(*) FROM inventory_flow WHERE part_request_id=?", pr1, "review rollback no flows");
            assertCount(0, "SELECT COUNT(*) FROM notification WHERE related_business_type='PART_REQUEST' AND related_business_id=?", pr1, "review rollback no notification");
            assertCount(0, "SELECT COUNT(*) FROM operation_log WHERE business_type='PART_REQUEST' AND business_id=?", pr1, "review rollback no log");
            execute("ALTER TABLE inventory_flow DROP CONSTRAINT ck_stage7_wh_review");

            // ---- review approve success ----
            warehouse.review(warehouseId, pr1, true, "approve");
            assertText("APPROVED", "SELECT status FROM part_request WHERE part_request_id=?", pr1, "review approve request");
            assertInventory(1, 20, 18, 2, 0, "review approve part1 locked");
            assertInventory(2, 8, 7, 1, 0, "review approve part2 locked");
            assertCount(2, "SELECT COUNT(*) FROM inventory_flow WHERE part_request_id=? AND flow_type='LOCK'", pr1, "review approve lock flows");
            assertCount(1, "SELECT COUNT(*) FROM notification WHERE notification_type='PART_RESULT' AND related_business_id=?", pr1, "review approve notification");

            // ---- issue rollback: fail at notification stage ----
            execute("ALTER TABLE notification ADD CONSTRAINT ck_stage7_wh_issue CHECK(title <> '配件已出库')");
            expectFailure(() -> warehouse.issue(warehouseId, pr1));
            assertInventory(1, 20, 18, 2, 0, "issue rollback part1");
            assertInventory(2, 8, 7, 1, 0, "issue rollback part2");
            assertText("APPROVED", "SELECT status FROM part_request WHERE part_request_id=?", pr1, "issue rollback request");
            assertCount(0, "SELECT COUNT(*) FROM inventory_flow WHERE part_request_id=? AND flow_type='OUT'", pr1, "issue rollback no OUT flows");
            assertText("0", "SELECT issued_quantity FROM part_request_item WHERE item_id=?", item1, "issue rollback item1 issued");
            assertCount(0, "SELECT COUNT(*) FROM notification WHERE notification_type='PART_ISSUED' AND related_business_id=?", pr1, "issue rollback no notification");
            execute("ALTER TABLE notification DROP CONSTRAINT ck_stage7_wh_issue");

            // ---- issue success ----
            warehouse.issue(warehouseId, pr1);
            assertText("ISSUED", "SELECT status FROM part_request WHERE part_request_id=?", pr1, "issue request");
            assertInventory(1, 18, 18, 0, 2, "issue part1 issued");
            assertInventory(2, 7, 7, 0, 1, "issue part2 issued");
            assertCount(2, "SELECT COUNT(*) FROM inventory_flow WHERE part_request_id=? AND flow_type='OUT'", pr1, "issue OUT flows");
            assertText("2", "SELECT issued_quantity FROM part_request_item WHERE item_id=?", item1, "issue item1 issued");
            assertCount(1, "SELECT COUNT(*) FROM notification WHERE notification_type='PART_ISSUED' AND related_business_id=?", pr1, "issue notification");

            // ---- returnPart rollback: fail at operation log ----
            execute("ALTER TABLE operation_log ADD CONSTRAINT ck_stage7_wh_return CHECK(operation_type <> 'RETURN_PART')");
            expectFailure(() -> warehouse.returnPart(warehouseId, item1, 1, "return"));
            assertInventory(1, 18, 18, 0, 2, "return rollback part1");
            assertText("0", "SELECT return_quantity FROM part_request_item WHERE item_id=?", item1, "return rollback item1");
            assertCount(0, "SELECT COUNT(*) FROM inventory_flow WHERE part_request_id=? AND flow_type='RETURN'", pr1, "return rollback no flow");
            execute("ALTER TABLE operation_log DROP CONSTRAINT ck_stage7_wh_return");

            // ---- returnPart success ----
            warehouse.returnPart(warehouseId, item1, 1, "return");
            assertInventory(1, 19, 19, 0, 1, "return part1 restored");
            assertText("1", "SELECT return_quantity FROM part_request_item WHERE item_id=?", item1, "return item1");
            assertCount(1, "SELECT COUNT(*) FROM inventory_flow WHERE part_request_id=? AND flow_type='RETURN'", pr1, "return flow");

            // ---- release: request #2 approve then rollback/success ----
            long pr2 = insert("INSERT INTO part_request(request_no,order_id,engineer_id,status,reason) VALUES(?,?,?,'PENDING','回归释放申请')",
                    "PR-TX-WH-2", orderId, engineerId);
            insert("INSERT INTO part_request_item(part_request_id,part_id,request_quantity) VALUES(?,1,1)", pr2);
            insert("INSERT INTO part_request_item(part_request_id,part_id,request_quantity) VALUES(?,2,1)", pr2);
            warehouse.review(warehouseId, pr2, true, "approve");
            assertInventory(1, 19, 18, 1, 1, "release setup part1 locked");
            assertInventory(2, 7, 6, 1, 1, "release setup part2 locked");
            execute("ALTER TABLE operation_log ADD CONSTRAINT ck_stage7_wh_release CHECK(operation_type <> 'RELEASE_PART')");
            expectFailure(() -> warehouse.release(warehouseId, pr2, "release"));
            assertInventory(1, 19, 18, 1, 1, "release rollback part1");
            assertInventory(2, 7, 6, 1, 1, "release rollback part2");
            assertText("APPROVED", "SELECT status FROM part_request WHERE part_request_id=?", pr2, "release rollback request");
            assertCount(0, "SELECT COUNT(*) FROM inventory_flow WHERE part_request_id=? AND flow_type='UNLOCK'", pr2, "release rollback no unlock flows");
            execute("ALTER TABLE operation_log DROP CONSTRAINT ck_stage7_wh_release");
            warehouse.release(warehouseId, pr2, "release");
            assertText("CANCELLED", "SELECT status FROM part_request WHERE part_request_id=?", pr2, "release request");
            assertInventory(1, 19, 19, 0, 1, "release part1 unlocked");
            assertInventory(2, 7, 7, 0, 1, "release part2 unlocked");
            assertCount(2, "SELECT COUNT(*) FROM inventory_flow WHERE part_request_id=? AND flow_type='UNLOCK'", pr2, "release unlock flows");

            // ---- stock rollback + success ----
            execute("ALTER TABLE operation_log ADD CONSTRAINT ck_stage7_wh_stock CHECK(operation_type <> 'IN')");
            expectFailure(() -> warehouse.stock(warehouseId, 1, 5, "RESTOCK", "restock"));
            assertInventory(1, 19, 19, 0, 1, "stock rollback part1");
            assertCount(0, "SELECT COUNT(*) FROM inventory_flow WHERE flow_type='IN'", "stock rollback no flow");
            execute("ALTER TABLE operation_log DROP CONSTRAINT ck_stage7_wh_stock");
            warehouse.stock(warehouseId, 1, 5, "RESTOCK", "restock");
            assertInventory(1, 24, 24, 0, 1, "stock restock part1");
            assertCount(1, "SELECT COUNT(*) FROM inventory_flow WHERE flow_type='IN' AND quantity=5", "stock IN flow");
            warehouse.stock(warehouseId, 1, -3, "ADJUST", "adjust");
            assertInventory(1, 21, 21, 0, 1, "stock adjust part1");
            assertCount(1, "SELECT COUNT(*) FROM inventory_flow WHERE flow_type='ADJUST' AND quantity=3", "stock ADJUST flow abs");

            // ---- complete (original semantics: two autocommit steps) ----
            warehouse.complete(warehouseId, pr1);
            assertText("COMPLETED", "SELECT status FROM part_request WHERE part_request_id=?", pr1, "complete request");
            assertCount(1, "SELECT COUNT(*) FROM operation_log WHERE operation_type='COMPLETE_PART_REQUEST' AND business_id=?", pr1, "complete log");

            // ---- read-only methods ----
            if (warehouse.requestData().isEmpty() || warehouse.inventory().isEmpty() || warehouse.flows().isEmpty()) {
                throw new AssertionError("Read-only warehouse methods failed");
            }

            System.out.println("STAGE7_WAREHOUSE_TX_OK proxy=ok reviewRollback=ok review=ok issueRollback=ok issue=ok returnRollback=ok return=ok releaseRollback=ok release=ok stockRollback=ok stock=ok complete=ok math=ok readOnly=ok");
        }
    }

    private static void assertInventory(long partId, int total, int available, int locked, int issued, String label) throws Exception {
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement("SELECT total_quantity,available_quantity,locked_quantity,issued_quantity FROM part_inventory WHERE part_id=?")) {
            ps.setLong(1, partId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new AssertionError("No inventory row: part " + partId);
                int t = rs.getInt(1), a = rs.getInt(2), l = rs.getInt(3), i = rs.getInt(4);
                if (t != total || a != available || l != locked || i != issued)
                    throw new AssertionError(label + ": expected total=" + total + " available=" + available + " locked=" + locked + " issued=" + issued + ", actual total=" + t + " available=" + a + " locked=" + l + " issued=" + i);
                if (t != a + l) throw new AssertionError(label + ": inventory math broken, total=" + t + " available+locked=" + (a + l));
            }
        }
    }

    private interface Work { void run() throws Exception; }
    private static void expectFailure(Work work) throws Exception { boolean failed=false; try { work.run(); } catch (Exception expected) { failed=true; } if(!failed) throw new AssertionError("Expected failure did not occur"); }
    private static void assertCount(long expected, String sql, Object value, String label) throws Exception { long actual=scalar(sql,value); if(actual!=expected)throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static void assertCount(long expected, String sql, String label) throws Exception { long actual=scalar(sql); if(actual!=expected)throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static void assertText(String expected, String sql, Object value, String label) throws Exception { String actual=text(sql,value); if(!expected.equals(actual))throw new AssertionError(label+": expected="+expected+", actual="+actual); }
    private static long scalar(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new AssertionError("No row: "+sql);return rs.getLong(1);}} }
    private static String text(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new AssertionError("No row: "+sql);return rs.getString(1);}} }
    private static void execute(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql)){bind(ps,args);ps.executeUpdate();} }
    private static long insert(String sql, Object... args) throws Exception { try(Connection c=Database.open();PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){bind(ps,args);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){if(!rs.next())throw new AssertionError("No generated key");return rs.getLong(1);}} }
    private static void bind(PreparedStatement ps,Object...args)throws Exception{for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);}
}
