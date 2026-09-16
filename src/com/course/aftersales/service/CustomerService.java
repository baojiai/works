package com.course.aftersales.service;

import com.course.aftersales.mapper.AppointmentMapper;
import com.course.aftersales.mapper.RepairOrderMapper;
import com.course.aftersales.mapper.RepairRequestMapper;
import com.course.aftersales.mapper.ReviewMapper;
import com.course.aftersales.model.SessionUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerService {
    private final RepairRequestMapper repairRequestMapper;
    private final AppointmentMapper appointmentMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final ReviewMapper reviewMapper;

    @Autowired
    public CustomerService(RepairRequestMapper repairRequestMapper,
                           AppointmentMapper appointmentMapper,
                           RepairOrderMapper repairOrderMapper,
                           ReviewMapper reviewMapper) {
        this.repairRequestMapper = repairRequestMapper;
        this.appointmentMapper = appointmentMapper;
        this.repairOrderMapper = repairOrderMapper;
        this.reviewMapper = reviewMapper;
    }

    @Transactional(readOnly = true)
    public Map<String,List<Map<String,Object>>> formData() throws Exception {
        Map<String,List<Map<String,Object>>> data = new HashMap<>();
        data.put("devices", repairRequestMapper.findActiveDevices());
        data.put("faults", repairRequestMapper.findActiveFaults());
        data.put("areas", repairRequestMapper.findActiveAreas());
        data.put("slots", appointmentMapper.findActiveSlots());
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public long createRequest(long customerId,long deviceId,long faultId,long areaId,String description,String address,String phone,java.sql.Date expectedDate,Long slotId) throws Exception {
        if(deviceId<=0||faultId<=0||areaId<=0||description.isEmpty()||address.isEmpty()||phone.isEmpty()||expectedDate==null) throw new IllegalArgumentException("请完整填写报修信息");
        if(expectedDate.before(java.sql.Date.valueOf(LocalDate.now()))) throw new IllegalArgumentException("期望服务日期不能早于今天");
        if(repairRequestMapper.findMatchingActiveFault(faultId,deviceId)==null) throw new IllegalArgumentException("故障类别与设备类别不匹配");
        Map<String,Object> request=new LinkedHashMap<>();
        request.put("customerId",customerId); request.put("deviceId",deviceId); request.put("faultId",faultId);
        request.put("areaId",areaId); request.put("description",description); request.put("address",address);
        request.put("phone",phone); request.put("expectedDate",expectedDate); request.put("slotId",slotId);
        repairRequestMapper.insertRepairRequest(request);
        return ((Number)request.get("requestId")).longValue();
    }

    @Transactional(readOnly = true)
    public Map<String,Object> request(long customerId,long requestId) throws Exception {
        return repairRequestMapper.findRequest(customerId,requestId);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> candidates(long customerId,long requestId,String sort) throws Exception {
        return appointmentMapper.findCandidates(customerId,requestId,sort);
    }

    @Transactional(rollbackFor = Exception.class)
    public long book(final long customerId,final long requestId,final long engineerId,final long scheduleId,final long replacesId) throws Exception {
        AppointmentMapper mapper=appointmentMapper;
        Map<String,Object> match=mapper.findBookableMatch(customerId,requestId,engineerId,scheduleId);
        if(match==null) throw new IllegalStateException("工程师或时段已失效，请重新选择");
        if("BOOKED".equals(match.get("request_status")) && replacesId<=0) throw new IllegalStateException("该维修需求已有有效预约，请通过改约流程重新选择");
        if(mapper.occupySchedule(scheduleId)!=1) throw new IllegalStateException("该时段刚刚被占用，请重新选择");
        String suffix=String.valueOf(System.currentTimeMillis());
        Map<String,Object> appointment=new LinkedHashMap<>();
        appointment.put("appointmentNo","AP"+suffix); appointment.put("requestId",requestId);
        appointment.put("customerId",customerId); appointment.put("engineerId",engineerId);
        appointment.put("scheduleId",scheduleId); appointment.put("previousAppointmentId",replacesId>0?replacesId:null);
        mapper.insertAppointment(appointment);
        long appointmentId=((Number)appointment.get("appointmentId")).longValue();
        Map<String,Object> order=new LinkedHashMap<>();
        order.put("orderNo","RO"+suffix); order.put("appointmentId",appointmentId);
        order.put("customerId",customerId); order.put("engineerId",engineerId);
        mapper.insertRepairOrder(order);
        long orderId=((Number)order.get("orderId")).longValue();
        mapper.insertCreatedOrderLog(orderId,customerId);
        mapper.markRequestBooked(requestId);
        mapper.insertNewAppointmentNotification(engineerId,appointmentId);
        if(replacesId>0) closeOldAppointment(mapper,customerId,replacesId,appointmentId);
        mapper.insertOperationLog(customerId,"CREATE_APPOINTMENT","APPOINTMENT",appointmentId,"客户自主选择工程师并创建预约");
        return appointmentId;
    }

    private void closeOldAppointment(AppointmentMapper mapper,long customerId,long oldId,long newId) {
        Map<String,Object> old=mapper.findReplaceableAppointment(customerId,oldId);
        if(old==null) throw new IllegalStateException("原预约当前不可改约");
        String status=String.valueOf(old.get("status"));
        mapper.markAppointmentRescheduled(oldId);
        if("BOOKED".equals(status)) mapper.releaseOccupiedSchedule(old.get("schedule_id"));
        mapper.cancelOrder(old.get("order_id"));
        mapper.insertRescheduleChange(oldId,status,customerId,newId);
        mapper.insertRescheduleOrderLog(old.get("order_id"),customerId,old.get("order_status"));
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> appointments(SessionUser user) throws Exception {
        if("ENGINEER".equals(user.getRole())) return appointmentMapper.findAppointmentsForEngineer(user.getId());
        if(user.hasRole("CUSTOMER")) return appointmentMapper.findAppointmentsForCustomer(user.getId());
        return appointmentMapper.findAllAppointments();
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(long customerId,long appointmentId,String reason) throws Exception {
        if(reason.isEmpty()) throw new IllegalArgumentException("请填写取消原因");
        AppointmentMapper mapper=appointmentMapper;
        Map<String,Object> row=mapper.findCancelableAppointment(customerId,appointmentId);
        if(row==null) throw new IllegalStateException("当前预约不可取消");
        LocalDate date=((java.sql.Date)row.get("service_date")).toLocalDate();
        LocalTime time=((java.sql.Time)row.get("start_time")).toLocalTime();
        int hours=configInt(mapper,"CANCEL_HOURS",2);
        if(LocalDateTime.now().plusHours(hours).isAfter(LocalDateTime.of(date,time))) throw new IllegalStateException("已超过取消截止时间");
        mapper.markAppointmentCancelled(appointmentId,reason);
        mapper.releaseOccupiedSchedule(row.get("schedule_id"));
        mapper.cancelOrder(row.get("order_id"));
        mapper.insertCancelChange(appointmentId,customerId,reason);
        mapper.insertCancelOrderLog(row.get("order_id"),customerId,reason);
        mapper.insertOperationLog(customerId,"CANCEL_APPOINTMENT","APPOINTMENT",appointmentId,reason);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> orders(SessionUser user) throws Exception {
        if("ENGINEER".equals(user.getRole())) return repairOrderMapper.findOrdersForEngineer(user.getId());
        if(user.hasRole("CUSTOMER")) return repairOrderMapper.findOrdersForCustomer(user.getId());
        return repairOrderMapper.findAllOrders();
    }

    @Transactional(readOnly = true)
    public Map<String,Object> orderDetail(SessionUser user,long orderId) throws Exception {
        Map<String,Object> order;
        if("ENGINEER".equals(user.getRole())) order=repairOrderMapper.findOrderDetailForEngineer(orderId,user.getId());
        else if(user.hasRole("CUSTOMER")) order=repairOrderMapper.findOrderDetailForCustomer(orderId,user.getId());
        else order=repairOrderMapper.findOrderDetail(orderId);
        if(order==null) throw new SecurityException("无权查看该工单");
        Map<String,Object> result=new HashMap<>();
        result.put("order",order);
        result.put("records",repairOrderMapper.findRepairRecords(orderId));
        result.put("logs",repairOrderMapper.findStatusLogs(orderId));
        result.put("parts",repairOrderMapper.findPartUsage(orderId));
        result.put("acceptances",repairOrderMapper.findAcceptances(orderId));
        result.put("review",repairOrderMapper.findReview(orderId));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void accept(long customerId,long orderId,boolean passed,String comment) throws Exception {
        ReviewMapper mapper=reviewMapper;
        Map<String,Object> order=mapper.findOrderForAcceptance(customerId,orderId);
        if(order==null||!"PENDING_ACCEPTANCE".equals(order.get("order_status"))) throw new IllegalStateException("该工单当前不可验收");
        if(!passed&&comment.trim().isEmpty()) throw new IllegalArgumentException("验收不通过时必须填写原因");
        Map<String,Object> acceptance=new LinkedHashMap<>();
        acceptance.put("orderId",orderId); acceptance.put("customerId",customerId);
        acceptance.put("result",passed?"PASSED":"FAILED"); acceptance.put("comment",comment);
        mapper.insertAcceptance(acceptance);
        long acceptanceId=((Number)acceptance.get("acceptanceId")).longValue();
        String next=passed?"COMPLETED":"REWORK";
        if(passed) mapper.markOrderCompleted(orderId); else mapper.markOrderRework(orderId);
        mapper.insertAcceptanceStatusLog(orderId,customerId,next,comment);
        long engineerId=((Number)order.get("engineer_id")).longValue();
        if(passed) {
            mapper.markAppointmentFulfilled(order.get("appointment_id"));
            refreshEngineerStats(mapper,engineerId);
        } else {
            mapper.insertRework(orderId,acceptanceId,comment);
            mapper.insertReworkNotification(order.get("engineer_id"),comment,orderId);
        }
        mapper.insertOperationLog(customerId,"ACCEPT_ORDER","ORDER",orderId,passed?"验收通过":"验收失败并进入返修");
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(long customerId,long orderId,int rating,String content) throws Exception {
        if(rating<1||rating>5) throw new IllegalArgumentException("评分应为1至5星");
        ReviewMapper mapper=reviewMapper;
        Map<String,Object> order=mapper.findOrderForReview(customerId,orderId);
        if(order==null) throw new IllegalStateException("仅已完成工单可评价");
        mapper.insertReview(orderId,customerId,order.get("engineer_id"),rating,content);
        refreshEngineerStats(mapper,((Number)order.get("engineer_id")).longValue());
        mapper.insertOperationLog(customerId,"CREATE_REVIEW","ORDER",orderId,"提交服务评价");
    }

    private void refreshEngineerStats(ReviewMapper mapper,long engineerId) {
        Map<String,Object> stats=mapper.findEngineerReviewStats(engineerId);
        Map<String,Object> fulfillment=mapper.findEngineerFulfillmentStats(engineerId);
        Number total=(Number)fulfillment.get("total"),ok=(Number)fulfillment.get("ok");
        double rate=total.longValue()==0?100.0:ok.doubleValue()*100.0/total.doubleValue();
        mapper.updateEngineerStats(engineerId,stats.get("completed_count"),stats.get("average_rating"),stats.get("review_count"),rate);
    }

    private int configInt(AppointmentMapper mapper,String key,int fallback) {
        String value=mapper.findConfigValue(key);
        try { return value==null?fallback:Integer.parseInt(value); }
        catch(Exception e) { return fallback; }
    }

}
