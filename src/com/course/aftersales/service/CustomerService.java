package com.course.aftersales.service;

import com.course.aftersales.model.SessionUser;
import com.course.aftersales.repository.Database;
import java.sql.*;
import java.time.*;
import java.util.*;

public class CustomerService {
    public Map<String,List<Map<String,Object>>> formData() throws Exception {
        Map<String,List<Map<String,Object>>> data = new HashMap<>();
        try(Connection c=Database.open()) {
            data.put("devices",Database.query(c,"SELECT device_type_id,name FROM device_type WHERE status='ACTIVE' ORDER BY name"));
            data.put("faults",Database.query(c,"SELECT f.fault_type_id,f.device_type_id,f.name,d.name device_name FROM fault_type f JOIN device_type d ON d.device_type_id=f.device_type_id WHERE f.status='ACTIVE' AND d.status='ACTIVE' ORDER BY d.name,f.name"));
            data.put("areas",Database.query(c,"SELECT service_area_id,name FROM service_area WHERE status='ACTIVE' ORDER BY name"));
            data.put("slots",Database.query(c,"SELECT slot_id,name,start_time,end_time FROM standard_time_slot WHERE status='ACTIVE' ORDER BY start_time"));
        }
        return data;
    }

    public long createRequest(long customerId,long deviceId,long faultId,long areaId,String description,String address,String phone,java.sql.Date expectedDate,Long slotId) throws Exception {
        if(deviceId<=0||faultId<=0||areaId<=0||description.isEmpty()||address.isEmpty()||phone.isEmpty()||expectedDate==null) throw new IllegalArgumentException("请完整填写报修信息");
        if(expectedDate.before(java.sql.Date.valueOf(LocalDate.now()))) throw new IllegalArgumentException("期望服务日期不能早于今天");
        try(Connection c=Database.open()) {
            Map<String,Object> valid=Database.one(c,"SELECT f.fault_type_id FROM fault_type f WHERE f.fault_type_id=? AND f.device_type_id=? AND f.status='ACTIVE'",faultId,deviceId);
            if(valid==null) throw new IllegalArgumentException("故障类别与设备类别不匹配");
            return Database.insert(c,"INSERT INTO repair_request(customer_id,device_type_id,fault_type_id,service_area_id,fault_description,service_address,contact_phone,expected_date,expected_slot_id,status) VALUES(?,?,?,?,?,?,?,?,?,'OPEN')",customerId,deviceId,faultId,areaId,description,address,phone,expectedDate,slotId);
        }
    }

    public Map<String,Object> request(long customerId,long requestId) throws Exception {
        try(Connection c=Database.open()) { return Database.one(c,"SELECT rr.*,dt.name device_name,ft.name fault_name,sa.name area_name FROM repair_request rr JOIN device_type dt ON dt.device_type_id=rr.device_type_id JOIN fault_type ft ON ft.fault_type_id=rr.fault_type_id JOIN service_area sa ON sa.service_area_id=rr.service_area_id WHERE rr.repair_request_id=? AND rr.customer_id=?",requestId,customerId); }
    }

    public List<Map<String,Object>> candidates(long customerId,long requestId,String sort) throws Exception {
        String order;
        if("rating".equals(sort)) order="ep.average_rating DESC,es.service_date,st.start_time";
        else if("service".equals(sort)) order="ep.completed_count DESC,es.service_date,st.start_time";
        else if("earliest".equals(sort)) order="es.service_date,st.start_time,ep.average_rating DESC";
        else order="(ep.average_rating*0.45+ep.fulfillment_rate/100*5*0.30+LEAST(ep.completed_count,50)/10*0.25) DESC,es.service_date,st.start_time";
        String sql="SELECT ep.engineer_id,u.display_name engineer_name,ep.bio,ep.average_rating,ep.review_count,ep.completed_count,ep.fulfillment_rate,"+
            "ft.name skill_name,esa.service_area_id,es.schedule_id,es.service_date,st.name slot_name,st.start_time,st.end_time "+
            "FROM repair_request rr JOIN engineer_skill esk ON esk.fault_type_id=rr.fault_type_id JOIN engineer_profile ep ON ep.engineer_id=esk.engineer_id "+
            "JOIN system_user u ON u.user_id=ep.engineer_id JOIN engineer_service_area esa ON esa.engineer_id=ep.engineer_id AND esa.service_area_id=rr.service_area_id "+
            "JOIN engineer_schedule es ON es.engineer_id=ep.engineer_id AND es.service_date>=rr.expected_date AND es.status='AVAILABLE' "+
            "JOIN standard_time_slot st ON st.slot_id=es.slot_id JOIN fault_type ft ON ft.fault_type_id=esk.fault_type_id "+
            "WHERE rr.repair_request_id=? AND rr.customer_id=? AND rr.status IN ('OPEN','BOOKED') AND ep.qualification_status='APPROVED' AND ep.employment_status='ACTIVE' AND u.status='ACTIVE' "+
            "AND ep.engineer_id<>rr.customer_id AND (rr.expected_slot_id IS NULL OR rr.expected_slot_id=es.slot_id) ORDER BY "+order;
        try(Connection c=Database.open()) { return Database.query(c,sql,requestId,customerId); }
    }

    public long book(final long customerId,final long requestId,final long engineerId,final long scheduleId,final long replacesId) throws Exception {
        final long[] created={0};
        Database.tx(c->{
            Map<String,Object> match=Database.one(c,"SELECT es.schedule_id,rr.status request_status FROM repair_request rr JOIN engineer_skill sk ON sk.fault_type_id=rr.fault_type_id AND sk.engineer_id=? JOIN engineer_service_area ea ON ea.service_area_id=rr.service_area_id AND ea.engineer_id=? JOIN engineer_profile ep ON ep.engineer_id=? JOIN system_user u ON u.user_id=ep.engineer_id JOIN engineer_schedule es ON es.schedule_id=? AND es.engineer_id=? WHERE rr.repair_request_id=? AND rr.customer_id=? AND rr.customer_id<>ep.engineer_id AND rr.status IN ('OPEN','BOOKED') AND es.status='AVAILABLE' AND es.service_date>=CURRENT_DATE AND ep.qualification_status='APPROVED' AND ep.employment_status='ACTIVE' AND u.status='ACTIVE'",engineerId,engineerId,engineerId,scheduleId,engineerId,requestId,customerId);
            if(match==null) throw new IllegalStateException("工程师或时段已失效，请重新选择");
            if("BOOKED".equals(match.get("request_status")) && replacesId<=0) throw new IllegalStateException("该维修需求已有有效预约，请通过改约流程重新选择");
            if(Database.update(c,"UPDATE engineer_schedule SET status='OCCUPIED',version_no=version_no+1 WHERE schedule_id=? AND status='AVAILABLE'",scheduleId)!=1) throw new IllegalStateException("该时段刚刚被占用，请重新选择");
            String suffix=String.valueOf(System.currentTimeMillis());
            long appointmentId=Database.insert(c,"INSERT INTO appointment(appointment_no,request_id,customer_id,engineer_id,schedule_id,status,previous_appointment_id) VALUES(?,?,?,?,?,'BOOKED',?)","AP"+suffix,requestId,customerId,engineerId,scheduleId,replacesId>0?replacesId:null);
            long orderId=Database.insert(c,"INSERT INTO repair_order(order_no,appointment_id,customer_id,engineer_id,order_status) VALUES(?,?,?,?,'PENDING_VISIT')","RO"+suffix,appointmentId,customerId,engineerId);
            Database.update(c,"INSERT INTO order_status_log(order_id,operator_id,old_status,new_status,reason) VALUES(?,?,NULL,'PENDING_VISIT','预约成功自动创建工单')",orderId,customerId);
            Database.update(c,"UPDATE repair_request SET status='BOOKED' WHERE repair_request_id=?",requestId);
            Database.update(c,"INSERT INTO notification(receiver_id,notification_type,title,content,related_business_type,related_business_id) VALUES(?,'NEW_APPOINTMENT','收到新的维修预约','客户已完成预约，请按时提供服务','APPOINTMENT',?)",engineerId,appointmentId);
            if(replacesId>0) closeOldAppointment(c,customerId,replacesId,appointmentId);
            log(c,customerId,"CREATE_APPOINTMENT","APPOINTMENT",appointmentId,"客户自主选择工程师并创建预约");
            created[0]=appointmentId;
        });
        return created[0];
    }

    private void closeOldAppointment(Connection c,long customerId,long oldId,long newId) throws Exception {
        Map<String,Object> old=Database.one(c,"SELECT a.status,a.schedule_id,ro.order_id,ro.order_status FROM appointment a JOIN repair_order ro ON ro.appointment_id=a.appointment_id WHERE a.appointment_id=? AND a.customer_id=? AND a.status IN ('BOOKED','PENDING_RESCHEDULE')",oldId,customerId);
        if(old==null) throw new IllegalStateException("原预约当前不可改约");
        String status=String.valueOf(old.get("status"));
        Database.update(c,"UPDATE appointment SET status='RESCHEDULED',updated_at=CURRENT_TIMESTAMP WHERE appointment_id=?",oldId);
        if("BOOKED".equals(status)) Database.update(c,"UPDATE engineer_schedule SET status='AVAILABLE',version_no=version_no+1 WHERE schedule_id=? AND status='OCCUPIED'",old.get("schedule_id"));
        Database.update(c,"UPDATE repair_order SET order_status='CANCELLED' WHERE order_id=?",old.get("order_id"));
        Database.update(c,"INSERT INTO appointment_change(appointment_id,change_type,old_status,new_status,operator_id,reason) VALUES(?,'CUSTOMER_RESCHEDULE',?,'RESCHEDULED',?,?)",oldId,status,customerId,"新预约ID："+newId);
        Database.update(c,"INSERT INTO order_status_log(order_id,operator_id,old_status,new_status,reason) VALUES(?,?,?,?,?)",old.get("order_id"),customerId,old.get("order_status"),"CANCELLED","客户改约，原工单关闭");
    }

    public List<Map<String,Object>> appointments(SessionUser user) throws Exception {
        String where="ENGINEER".equals(user.getRole())?"(a.customer_id=? OR a.engineer_id=?)":user.hasRole("CUSTOMER")?"a.customer_id=?":"1=?";
        String sql="SELECT a.appointment_id,a.appointment_no,a.request_id,a.customer_id,a.engineer_id,a.status,a.created_at,u.display_name engineer_name,cu.display_name customer_name,es.service_date,st.name slot_name,st.start_time,rr.service_address,ft.name fault_name,ro.order_id,ro.customer_id order_customer_id,ro.engineer_id order_engineer_id,ro.order_status FROM appointment a JOIN system_user u ON u.user_id=a.engineer_id JOIN system_user cu ON cu.user_id=a.customer_id JOIN engineer_schedule es ON es.schedule_id=a.schedule_id JOIN standard_time_slot st ON st.slot_id=es.slot_id JOIN repair_request rr ON rr.repair_request_id=a.request_id JOIN fault_type ft ON ft.fault_type_id=rr.fault_type_id JOIN repair_order ro ON ro.appointment_id=a.appointment_id WHERE "+where+" ORDER BY a.created_at DESC";
        try(Connection c=Database.open()){
            if("ENGINEER".equals(user.getRole())) return Database.query(c,sql,user.getId(),user.getId());
            return Database.query(c,sql,user.hasRole("CUSTOMER")?user.getId():1);
        }
    }

    public void cancel(long customerId,long appointmentId,String reason) throws Exception {
        if(reason.isEmpty()) throw new IllegalArgumentException("请填写取消原因");
        Database.tx(c->{
            Map<String,Object> row=Database.one(c,"SELECT a.status,a.schedule_id,ro.order_id,es.service_date,st.start_time FROM appointment a JOIN repair_order ro ON ro.appointment_id=a.appointment_id JOIN engineer_schedule es ON es.schedule_id=a.schedule_id JOIN standard_time_slot st ON st.slot_id=es.slot_id WHERE a.appointment_id=? AND a.customer_id=? AND a.status='BOOKED' AND ro.order_status='PENDING_VISIT'",appointmentId,customerId);
            if(row==null) throw new IllegalStateException("当前预约不可取消");
            LocalDate d=((java.sql.Date)row.get("service_date")).toLocalDate(); LocalTime t=((Time)row.get("start_time")).toLocalTime();
            int hours=configInt(c,"CANCEL_HOURS",2);
            if(LocalDateTime.now().plusHours(hours).isAfter(LocalDateTime.of(d,t))) throw new IllegalStateException("已超过取消截止时间");
            Database.update(c,"UPDATE appointment SET status='CANCELLED',cancel_reason=?,updated_at=CURRENT_TIMESTAMP WHERE appointment_id=?",reason,appointmentId);
            Database.update(c,"UPDATE engineer_schedule SET status='AVAILABLE',version_no=version_no+1 WHERE schedule_id=? AND status='OCCUPIED'",row.get("schedule_id"));
            Database.update(c,"UPDATE repair_order SET order_status='CANCELLED' WHERE order_id=?",row.get("order_id"));
            Database.update(c,"INSERT INTO appointment_change(appointment_id,change_type,old_status,new_status,operator_id,reason) VALUES(?,'CUSTOMER_CANCEL','BOOKED','CANCELLED',?,?)",appointmentId,customerId,reason);
            Database.update(c,"INSERT INTO order_status_log(order_id,operator_id,old_status,new_status,reason) VALUES(?,?,'PENDING_VISIT','CANCELLED',?)",row.get("order_id"),customerId,reason);
            log(c,customerId,"CANCEL_APPOINTMENT","APPOINTMENT",appointmentId,reason);
        });
    }

    public List<Map<String,Object>> orders(SessionUser user) throws Exception {
        String where="ENGINEER".equals(user.getRole())?"(ro.customer_id=? OR ro.engineer_id=?)":user.hasRole("CUSTOMER")?"ro.customer_id=?":"1=?";
        String sql="SELECT ro.order_id,ro.order_no,ro.customer_id,ro.engineer_id,ro.order_status,ro.created_at,ro.started_at,ro.submitted_at,ro.completed_at,a.appointment_no,eu.display_name engineer_name,cu.display_name customer_name,ft.name fault_name,rr.service_address FROM repair_order ro JOIN appointment a ON a.appointment_id=ro.appointment_id JOIN repair_request rr ON rr.repair_request_id=a.request_id JOIN fault_type ft ON ft.fault_type_id=rr.fault_type_id JOIN system_user eu ON eu.user_id=ro.engineer_id JOIN system_user cu ON cu.user_id=ro.customer_id WHERE "+where+" ORDER BY ro.created_at DESC";
        try(Connection c=Database.open()){
            if("ENGINEER".equals(user.getRole())) return Database.query(c,sql,user.getId(),user.getId());
            return Database.query(c,sql,user.hasRole("CUSTOMER")?user.getId():1);
        }
    }

    public Map<String,Object> orderDetail(SessionUser user,long orderId) throws Exception {
        try(Connection c=Database.open()){
            String access="ENGINEER".equals(user.getRole())?"(ro.customer_id=? OR ro.engineer_id=?)":user.hasRole("CUSTOMER")?"ro.customer_id=?":user.hasRole("ENGINEER")?"ro.engineer_id=?":"1=?";
            Map<String,Object> result=new HashMap<>();
            Map<String,Object> order="ENGINEER".equals(user.getRole())
                ? Database.one(c,"SELECT ro.*,a.appointment_no,rr.fault_description,rr.service_address,rr.contact_phone,dt.name device_name,ft.name fault_name,eu.display_name engineer_name,cu.display_name customer_name FROM repair_order ro JOIN appointment a ON a.appointment_id=ro.appointment_id JOIN repair_request rr ON rr.repair_request_id=a.request_id JOIN device_type dt ON dt.device_type_id=rr.device_type_id JOIN fault_type ft ON ft.fault_type_id=rr.fault_type_id JOIN system_user eu ON eu.user_id=ro.engineer_id JOIN system_user cu ON cu.user_id=ro.customer_id WHERE ro.order_id=? AND "+access,orderId,user.getId(),user.getId())
                : Database.one(c,"SELECT ro.*,a.appointment_no,rr.fault_description,rr.service_address,rr.contact_phone,dt.name device_name,ft.name fault_name,eu.display_name engineer_name,cu.display_name customer_name FROM repair_order ro JOIN appointment a ON a.appointment_id=ro.appointment_id JOIN repair_request rr ON rr.repair_request_id=a.request_id JOIN device_type dt ON dt.device_type_id=rr.device_type_id JOIN fault_type ft ON ft.fault_type_id=rr.fault_type_id JOIN system_user eu ON eu.user_id=ro.engineer_id JOIN system_user cu ON cu.user_id=ro.customer_id WHERE ro.order_id=? AND "+access,orderId,user.hasRole("CUSTOMER")||user.hasRole("ENGINEER")?user.getId():1);
            if(order==null) throw new SecurityException("无权查看该工单");
            result.put("order",order);
            result.put("records",Database.query(c,"SELECT rr.*,u.display_name engineer_name FROM repair_record rr JOIN system_user u ON u.user_id=rr.engineer_id WHERE rr.order_id=? ORDER BY rr.created_at",orderId));
            result.put("logs",Database.query(c,"SELECT l.*,u.display_name operator_name FROM order_status_log l JOIN system_user u ON u.user_id=l.operator_id WHERE l.order_id=? ORDER BY l.created_at",orderId));
            result.put("parts",Database.query(c,"SELECT p.name,p.model,i.request_quantity,i.issued_quantity,i.return_quantity,pr.status FROM part_request pr JOIN part_request_item i ON i.part_request_id=pr.part_request_id JOIN part p ON p.part_id=i.part_id WHERE pr.order_id=?",orderId));
            result.put("acceptances",Database.query(c,"SELECT * FROM acceptance WHERE order_id=? ORDER BY created_at",orderId));
            result.put("review",Database.one(c,"SELECT * FROM review WHERE order_id=? AND status='VALID'",orderId));
            return result;
        }
    }

    public void accept(long customerId,long orderId,boolean passed,String comment) throws Exception {
        Database.tx(c->{
            Map<String,Object> order=Database.one(c,"SELECT order_status,engineer_id,appointment_id FROM repair_order WHERE order_id=? AND customer_id=?",orderId,customerId);
            if(order==null||!"PENDING_ACCEPTANCE".equals(order.get("order_status"))) throw new IllegalStateException("该工单当前不可验收");
            if(!passed&&comment.trim().isEmpty()) throw new IllegalArgumentException("验收不通过时必须填写原因");
            long acceptanceId=Database.insert(c,"INSERT INTO acceptance(order_id,customer_id,result,comment) VALUES(?,?,?,?)",orderId,customerId,passed?"PASSED":"FAILED",comment);
            String next=passed?"COMPLETED":"REWORK";
            Database.update(c,"UPDATE repair_order SET order_status=?,completed_at="+(passed?"CURRENT_TIMESTAMP":"NULL")+" WHERE order_id=?",next,orderId);
            Database.update(c,"INSERT INTO order_status_log(order_id,operator_id,old_status,new_status,reason) VALUES(?,?,'PENDING_ACCEPTANCE',?,?)",orderId,customerId,next,comment);
            if(passed){ Database.update(c,"UPDATE appointment SET status='FULFILLED',updated_at=CURRENT_TIMESTAMP WHERE appointment_id=?",order.get("appointment_id")); refreshEngineerStats(c,((Number)order.get("engineer_id")).longValue()); }
            else { Database.update(c,"INSERT INTO rework(order_id,acceptance_id,reason,status) VALUES(?,?,?,'IN_PROGRESS')",orderId,acceptanceId,comment); Database.update(c,"INSERT INTO notification(receiver_id,notification_type,title,content,related_business_type,related_business_id) VALUES(?,'REWORK','客户验收未通过',?,'ORDER',?)",order.get("engineer_id"),comment,orderId); }
            log(c,customerId,"ACCEPT_ORDER","ORDER",orderId,passed?"验收通过":"验收失败并进入返修");
        });
    }

    public void review(long customerId,long orderId,int rating,String content) throws Exception {
        if(rating<1||rating>5) throw new IllegalArgumentException("评分应为1至5星");
        Database.tx(c->{
            Map<String,Object> order=Database.one(c,"SELECT engineer_id FROM repair_order WHERE order_id=? AND customer_id=? AND order_status='COMPLETED'",orderId,customerId);
            if(order==null) throw new IllegalStateException("仅已完成工单可评价");
            Database.insert(c,"INSERT INTO review(order_id,customer_id,engineer_id,rating,content,status) VALUES(?,?,?,?,?,'VALID')",orderId,customerId,order.get("engineer_id"),rating,content);
            refreshEngineerStats(c,((Number)order.get("engineer_id")).longValue());
            log(c,customerId,"CREATE_REVIEW","ORDER",orderId,"提交服务评价");
        });
    }

    private void refreshEngineerStats(Connection c,long engineerId) throws Exception {
        Map<String,Object> stats=Database.one(c,"SELECT COUNT(DISTINCT CASE WHEN ro.order_status='COMPLETED' THEN ro.order_id END) completed_count,COALESCE(AVG(CASE WHEN rv.status='VALID' THEN rv.rating END),0) average_rating,COUNT(DISTINCT CASE WHEN rv.status='VALID' THEN rv.review_id END) review_count FROM repair_order ro LEFT JOIN review rv ON rv.order_id=ro.order_id WHERE ro.engineer_id=?",engineerId);
        Map<String,Object> fulfillment=Database.one(c,"SELECT SUM(CASE WHEN status='FULFILLED' THEN 1 ELSE 0 END) ok,COUNT(*) total FROM appointment WHERE engineer_id=? AND status IN ('FULFILLED','PENDING_RESCHEDULE')",engineerId);
        Number total=(Number)fulfillment.get("total"),ok=(Number)fulfillment.get("ok"); double rate=total.longValue()==0?100.0:ok.doubleValue()*100.0/total.doubleValue();
        Database.update(c,"UPDATE engineer_profile SET completed_count=?,average_rating=?,review_count=?,fulfillment_rate=? WHERE engineer_id=?",stats.get("completed_count"),stats.get("average_rating"),stats.get("review_count"),rate,engineerId);
    }
    private int configInt(Connection c,String key,int fallback) throws Exception { Map<String,Object> row=Database.one(c,"SELECT config_value FROM system_config WHERE config_key=?",key); try{return row==null?fallback:Integer.parseInt(String.valueOf(row.get("config_value")));}catch(Exception e){return fallback;} }
    private void log(Connection c,long userId,String op,String type,long id,String text) throws Exception { Database.update(c,"INSERT INTO operation_log(user_id,operation_type,business_type,business_id,description) VALUES(?,?,?,?,?)",userId,op,type,id,text); }
}
