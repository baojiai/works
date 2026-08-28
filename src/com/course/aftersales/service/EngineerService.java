package com.course.aftersales.service;

import com.course.aftersales.repository.Database;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class EngineerService {
    public Map<String,Object> profileData(long engineerId)throws Exception{
        Map<String,Object> data=new HashMap<>();
        try(Connection c=Database.open()){
            data.put("profile",Database.one(c,"SELECT u.display_name,u.phone,ep.* FROM engineer_profile ep JOIN system_user u ON u.user_id=ep.engineer_id WHERE ep.engineer_id=?",engineerId));
            data.put("faults",Database.query(c,"SELECT f.fault_type_id,f.name,CASE WHEN s.engineer_id IS NULL THEN FALSE ELSE TRUE END selected FROM fault_type f LEFT JOIN engineer_skill s ON s.fault_type_id=f.fault_type_id AND s.engineer_id=? WHERE f.status='ACTIVE' ORDER BY f.name",engineerId));
            data.put("areas",Database.query(c,"SELECT a.service_area_id,a.name,CASE WHEN e.engineer_id IS NULL THEN FALSE ELSE TRUE END selected FROM service_area a LEFT JOIN engineer_service_area e ON e.service_area_id=a.service_area_id AND e.engineer_id=? WHERE a.status='ACTIVE' ORDER BY a.name",engineerId));
        }return data;
    }
    public void updateProfile(final long engineerId,final String phone,final String bio,final String[] faults,final String[] areas)throws Exception{
        Database.tx(c->{
            Database.update(c,"UPDATE system_user SET phone=? WHERE user_id=?",phone,engineerId); Database.update(c,"UPDATE engineer_profile SET bio=? WHERE engineer_id=?",bio,engineerId);
            Database.update(c,"DELETE FROM engineer_skill WHERE engineer_id=?",engineerId); if(faults!=null)for(String id:faults)Database.update(c,"INSERT INTO engineer_skill(engineer_id,fault_type_id,proficiency_level) VALUES(?,?,3)",engineerId,Long.parseLong(id));
            Database.update(c,"DELETE FROM engineer_service_area WHERE engineer_id=?",engineerId); if(areas!=null)for(String id:areas)Database.update(c,"INSERT INTO engineer_service_area(engineer_id,service_area_id) VALUES(?,?)",engineerId,Long.parseLong(id));
            log(c,engineerId,"UPDATE_PROFILE","ENGINEER",engineerId,"更新工程师档案、技能和服务区域");
        });
    }
    public Map<String,Object> scheduleData(long engineerId)throws Exception{
        Map<String,Object> data=new HashMap<>();try(Connection c=Database.open()){
            data.put("slots",Database.query(c,"SELECT * FROM standard_time_slot WHERE status='ACTIVE' ORDER BY start_time"));
            data.put("schedules",Database.query(c,"SELECT es.*,st.name slot_name,st.start_time,st.end_time FROM engineer_schedule es JOIN standard_time_slot st ON st.slot_id=es.slot_id WHERE es.engineer_id=? AND es.service_date>=CURRENT_DATE ORDER BY es.service_date,st.start_time",engineerId));
        }return data;
    }
    public void addSchedule(long engineerId,java.sql.Date date,long slotId)throws Exception{
        if(date==null||date.before(java.sql.Date.valueOf(LocalDate.now())))throw new IllegalArgumentException("排班日期不能早于今天");
        try(Connection c=Database.open()){
            Map<String,Object> engineer=Database.one(c,"SELECT ep.engineer_id FROM engineer_profile ep JOIN system_user u ON u.user_id=ep.engineer_id WHERE ep.engineer_id=? AND ep.qualification_status='APPROVED' AND ep.employment_status='ACTIVE' AND u.status='ACTIVE'",engineerId);
            if(engineer==null)throw new IllegalStateException("当前资质或账号状态不可发布排班");
            Database.insert(c,"INSERT INTO engineer_schedule(engineer_id,service_date,slot_id,status) VALUES(?,?,?,'AVAILABLE')",engineerId,date,slotId);
            log(c,engineerId,"CREATE_SCHEDULE","SCHEDULE",0,"发布可预约时段");
        }catch(SQLException e){if(e.getSQLState()!=null&&e.getSQLState().startsWith("23"))throw new IllegalStateException("同一天的该标准时段已存在");throw e;}
    }
    public void closeSchedule(long engineerId,long scheduleId)throws Exception{
        try(Connection c=Database.open()){if(Database.update(c,"UPDATE engineer_schedule SET status='CLOSED',version_no=version_no+1 WHERE schedule_id=? AND engineer_id=? AND status='AVAILABLE'",scheduleId,engineerId)!=1)throw new IllegalStateException("仅空闲时段可关闭"); log(c,engineerId,"CLOSE_SCHEDULE","SCHEDULE",scheduleId,"关闭空闲时段");}
    }
    public void abnormalCancel(final long engineerId,final long appointmentId,final String reason)throws Exception{
        if(reason.isEmpty())throw new IllegalArgumentException("必须填写异常取消原因");
        Database.tx(c->{
            Map<String,Object> row=Database.one(c,"SELECT a.customer_id,a.schedule_id,ro.order_id FROM appointment a JOIN repair_order ro ON ro.appointment_id=a.appointment_id WHERE a.appointment_id=? AND a.engineer_id=? AND a.status='BOOKED' AND ro.order_status='PENDING_VISIT'",appointmentId,engineerId);
            if(row==null)throw new IllegalStateException("只有尚未开始的预约可以异常取消");
            Database.update(c,"UPDATE appointment SET status='PENDING_RESCHEDULE',cancel_reason=?,updated_at=CURRENT_TIMESTAMP WHERE appointment_id=?",reason,appointmentId);
            Database.update(c,"UPDATE engineer_schedule SET status='AVAILABLE',version_no=version_no+1 WHERE schedule_id=?",row.get("schedule_id"));
            Database.update(c,"UPDATE repair_order SET order_status='PENDING_RESCHEDULE' WHERE order_id=?",row.get("order_id"));
            Database.update(c,"INSERT INTO appointment_change(appointment_id,change_type,old_status,new_status,operator_id,reason) VALUES(?,'ENGINEER_CANCEL','BOOKED','PENDING_RESCHEDULE',?,?)",appointmentId,engineerId,reason);
            Database.update(c,"INSERT INTO order_status_log(order_id,operator_id,old_status,new_status,reason) VALUES(?,?,'PENDING_VISIT','PENDING_RESCHEDULE',?)",row.get("order_id"),engineerId,reason);
            Database.update(c,"INSERT INTO notification(receiver_id,notification_type,title,content,related_business_type,related_business_id) VALUES(?,'RESCHEDULE_REQUIRED','工程师无法按约服务，请重新选择',?,'APPOINTMENT',?)",row.get("customer_id"),reason,appointmentId);
            log(c,engineerId,"ENGINEER_CANCEL","APPOINTMENT",appointmentId,reason);
        });
    }
    public void saveRecord(long engineerId,long orderId,String diagnosis,String action,double hours,String remark)throws Exception{
        if(diagnosis.isEmpty()||action.isEmpty()||hours<0)throw new IllegalArgumentException("请完整填写诊断、维修措施和工时");
        try(Connection c=Database.open()){
            Map<String,Object> row=Database.one(c,"SELECT order_id FROM repair_order WHERE order_id=? AND engineer_id=? AND order_status IN ('REPAIRING','WAITING_PARTS','REWORK')",orderId,engineerId);if(row==null)throw new IllegalStateException("当前工单不可记录维修过程");
            Database.insert(c,"INSERT INTO repair_record(order_id,engineer_id,diagnosis,repair_action,labor_hours,remark) VALUES(?,?,?,?,?,?)",orderId,engineerId,diagnosis,action,hours,remark);log(c,engineerId,"ADD_REPAIR_RECORD","ORDER",orderId,"追加维修过程记录");
        }
    }
    public void transition(final long engineerId,final long orderId,final String action)throws Exception{
        final Map<String,String[]> rules=new HashMap<>();rules.put("START",new String[]{"PENDING_VISIT","REPAIRING"});rules.put("WAIT_PARTS",new String[]{"REPAIRING","WAITING_PARTS"});rules.put("RESUME",new String[]{"WAITING_PARTS","REPAIRING"});rules.put("FINISH",new String[]{"REPAIRING,REWORK","PENDING_ACCEPTANCE"});
        String[] rule=rules.get(action);if(rule==null)throw new IllegalArgumentException("未知工单操作");
        Database.tx(c->{
            Map<String,Object> row=Database.one(c,"SELECT order_status,customer_id FROM repair_order WHERE order_id=? AND engineer_id=?",orderId,engineerId);if(row==null)throw new SecurityException("无权操作该工单");
            String old=String.valueOf(row.get("order_status"));if(!Arrays.asList(rule[0].split(",")).contains(old))throw new IllegalStateException("工单不能从“"+old+"”执行该操作");
            if("FINISH".equals(action)){
                Map<String,Object> count=Database.one(c,"SELECT COUNT(*) c FROM repair_record WHERE order_id=?",orderId);if(((Number)count.get("c")).intValue()==0)throw new IllegalStateException("提交完工前至少填写一条维修记录");
                Map<String,Object> pending=Database.one(c,"SELECT COUNT(*) c FROM part_request WHERE order_id=? AND status IN ('PENDING','APPROVED')",orderId);if(((Number)pending.get("c")).intValue()>0)throw new IllegalStateException("仍有未完成的配件申请，暂不能完工");
            }
            String timeSql="START".equals(action)?",started_at=CURRENT_TIMESTAMP":"FINISH".equals(action)?",submitted_at=CURRENT_TIMESTAMP":"";
            Database.update(c,"UPDATE repair_order SET order_status=?"+timeSql+" WHERE order_id=?",rule[1],orderId);
            Database.update(c,"INSERT INTO order_status_log(order_id,operator_id,old_status,new_status,reason) VALUES(?,?,?,?,?)",orderId,engineerId,old,rule[1],"工程师执行"+action);
            if("FINISH".equals(action))Database.update(c,"INSERT INTO notification(receiver_id,notification_type,title,content,related_business_type,related_business_id) VALUES(?,'ACCEPTANCE_REQUIRED','维修已提交完工','请查看维修结果并完成验收','ORDER',?)",row.get("customer_id"),orderId);
            log(c,engineerId,"ORDER_"+action,"ORDER",orderId,old+" -> "+rule[1]);
        });
    }
    public List<Map<String,Object>> parts()throws Exception{try(Connection c=Database.open()){return Database.query(c,"SELECT p.part_id,p.part_code,p.name,p.model,p.unit,i.available_quantity FROM part p JOIN part_inventory i ON i.part_id=p.part_id WHERE p.status='ACTIVE' ORDER BY p.name");}}
    public void createPartRequest(final long engineerId,final long orderId,final String reason,final String[] partIds,final String[] quantities)throws Exception{
        if(reason.isEmpty()||partIds==null||quantities==null||partIds.length!=quantities.length)throw new IllegalArgumentException("请填写申请原因和至少一项配件");
        Database.tx(c->{
            Map<String,Object> order=Database.one(c,"SELECT order_id FROM repair_order WHERE order_id=? AND engineer_id=? AND order_status IN ('REPAIRING','WAITING_PARTS','REWORK')",orderId,engineerId);if(order==null)throw new IllegalStateException("当前工单不可申请配件");
            Map<String,Object> pending=Database.one(c,"SELECT COUNT(*) c FROM part_request WHERE order_id=? AND status='PENDING'",orderId);if(((Number)pending.get("c")).intValue()>0)throw new IllegalStateException("该工单已有待审核配件申请");
            String no="PR"+System.currentTimeMillis();long requestId=Database.insert(c,"INSERT INTO part_request(request_no,order_id,engineer_id,status,reason) VALUES(?,?,?,'PENDING',?)",no,orderId,engineerId,reason);
            int added=0;for(int i=0;i<partIds.length;i++){long partId=Long.parseLong(partIds[i]);int qty=Integer.parseInt(quantities[i]);if(partId>0&&qty>0){Database.insert(c,"INSERT INTO part_request_item(part_request_id,part_id,request_quantity) VALUES(?,?,?)",requestId,partId,qty);added++;}}
            if(added==0)throw new IllegalArgumentException("申请数量必须大于0");
            List<Map<String,Object>> managers=Database.query(c,"SELECT user_id FROM system_user WHERE role_type='WAREHOUSE' AND status='ACTIVE'");for(Map<String,Object> m:managers)Database.update(c,"INSERT INTO notification(receiver_id,notification_type,title,content,related_business_type,related_business_id) VALUES(?,'PART_REVIEW','新的配件申请待审核',?,'PART_REQUEST',?)",m.get("user_id"),no,requestId);
            log(c,engineerId,"CREATE_PART_REQUEST","PART_REQUEST",requestId,reason);
        });
    }
    private void log(Connection c,long user,String op,String type,long id,String text)throws Exception{Database.update(c,"INSERT INTO operation_log(user_id,operation_type,business_type,business_id,description) VALUES(?,?,?,?,?)",user,op,type,id,text);}
}
