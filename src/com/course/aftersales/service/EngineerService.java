package com.course.aftersales.service;

import com.course.aftersales.mapper.EngineerProfileMapper;
import com.course.aftersales.mapper.EngineerScheduleMapper;
import com.course.aftersales.mapper.EngineerWorkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EngineerService {
    private final EngineerProfileMapper profileMapper;
    private final EngineerScheduleMapper scheduleMapper;
    private final EngineerWorkMapper workMapper;

    @Autowired
    public EngineerService(EngineerProfileMapper profileMapper, EngineerScheduleMapper scheduleMapper, EngineerWorkMapper workMapper) {
        this.profileMapper=profileMapper; this.scheduleMapper=scheduleMapper; this.workMapper=workMapper;
    }

    @Transactional(readOnly = true)
    public Map<String,Object> profileData(long engineerId)throws Exception{
        Map<String,Object> data=new HashMap<>();
        data.put("profile",profileMapper.findProfile(engineerId));
        data.put("faults",profileMapper.findFaultOptions(engineerId));
        data.put("areas",profileMapper.findAreaOptions(engineerId));
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(final long engineerId,final String phone,final String bio,final String[] faults,final String[] areas)throws Exception{
        profileMapper.updateUserPhone(engineerId,phone); profileMapper.updateProfileBio(engineerId,bio); profileMapper.deleteSkills(engineerId);
        if(faults!=null)for(String id:faults)profileMapper.insertSkill(engineerId,Long.parseLong(id));
        profileMapper.deleteAreas(engineerId); if(areas!=null)for(String id:areas)profileMapper.insertArea(engineerId,Long.parseLong(id));
        profileMapper.insertOperationLog(engineerId,"UPDATE_PROFILE","ENGINEER",engineerId,"更新工程师档案、技能和服务区域");
    }

    @Transactional(readOnly = true)
    public Map<String,Object> scheduleData(long engineerId)throws Exception{
        Map<String,Object> data=new HashMap<>(); data.put("slots",scheduleMapper.findActiveSlots()); data.put("schedules",scheduleMapper.findSchedules(engineerId)); return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addSchedule(long engineerId,java.sql.Date date,long slotId)throws Exception{
        if(date==null||date.before(java.sql.Date.valueOf(LocalDate.now())))throw new IllegalArgumentException("排班日期不能早于今天");
        if(scheduleMapper.findEligibleEngineerId(engineerId)==null)throw new IllegalStateException("当前资质或账号状态不可发布排班");
        try{scheduleMapper.insertSchedule(engineerId,date,slotId);}catch(RuntimeException e){if(isConstraintViolation(e))throw new IllegalStateException("同一天的该标准时段已存在");throw e;}
        scheduleMapper.insertOperationLog(engineerId,"CREATE_SCHEDULE","SCHEDULE",0,"发布可预约时段");
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeSchedule(long engineerId,long scheduleId)throws Exception{
        if(scheduleMapper.closeSchedule(engineerId,scheduleId)!=1)throw new IllegalStateException("仅空闲时段可关闭");
        scheduleMapper.insertOperationLog(engineerId,"CLOSE_SCHEDULE","SCHEDULE",scheduleId,"关闭空闲时段");
    }

    @Transactional(rollbackFor = Exception.class)
    public void abnormalCancel(final long engineerId,final long appointmentId,final String reason)throws Exception{
        if(reason.isEmpty())throw new IllegalArgumentException("必须填写异常取消原因");
        Map<String,Object> row=workMapper.findCancelableAppointment(engineerId,appointmentId); if(row==null)throw new IllegalStateException("只有尚未开始的预约可以异常取消");
        workMapper.markAppointmentPendingReschedule(appointmentId,reason); workMapper.releaseSchedule(row.get("schedule_id")); workMapper.markOrderPendingReschedule(row.get("order_id"));
        workMapper.insertEngineerCancelChange(appointmentId,engineerId,reason); workMapper.insertPendingRescheduleLog(row.get("order_id"),engineerId,reason);
        workMapper.insertRescheduleNotification(row.get("customer_id"),reason,appointmentId); workMapper.insertOperationLog(engineerId,"ENGINEER_CANCEL","APPOINTMENT",appointmentId,reason);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveRecord(long engineerId,long orderId,String diagnosis,String action,double hours,String remark)throws Exception{
        if(diagnosis.isEmpty()||action.isEmpty()||hours<0)throw new IllegalArgumentException("请完整填写诊断、维修措施和工时");
        if(workMapper.findRecordableOrderId(engineerId,orderId)==null)throw new IllegalStateException("当前工单不可记录维修过程");
        workMapper.insertRepairRecord(engineerId,orderId,diagnosis,action,hours,remark);
        workMapper.insertOperationLog(engineerId,"ADD_REPAIR_RECORD","ORDER",orderId,"追加维修过程记录");
    }

    @Transactional(rollbackFor = Exception.class)
    public void transition(final long engineerId,final long orderId,final String action)throws Exception{
        final Map<String,String[]> rules=new HashMap<>(); rules.put("START",new String[]{"PENDING_VISIT","REPAIRING"}); rules.put("WAIT_PARTS",new String[]{"REPAIRING","WAITING_PARTS"}); rules.put("RESUME",new String[]{"WAITING_PARTS","REPAIRING"}); rules.put("FINISH",new String[]{"REPAIRING,REWORK","PENDING_ACCEPTANCE"});
        String[] rule=rules.get(action); if(rule==null)throw new IllegalArgumentException("未知工单操作");
        Map<String,Object> row=workMapper.findOrderForTransition(engineerId,orderId); if(row==null)throw new SecurityException("无权操作该工单");
        String old=String.valueOf(row.get("order_status")); if(!Arrays.asList(rule[0].split(",")).contains(old))throw new IllegalStateException("工单不能从“"+old+"”执行该操作");
        if("FINISH".equals(action)){if(workMapper.countRepairRecords(orderId)==0)throw new IllegalStateException("提交完工前至少填写一条维修记录");if(workMapper.countOpenPartRequests(orderId)>0)throw new IllegalStateException("仍有未完成的配件申请，暂不能完工");}
        workMapper.updateOrderStatus(orderId,rule[1],action); workMapper.insertOrderStatusLog(orderId,engineerId,old,rule[1],"工程师执行"+action);
        if("FINISH".equals(action))workMapper.insertAcceptanceNotification(row.get("customer_id"),orderId);
        workMapper.insertOperationLog(engineerId,"ORDER_"+action,"ORDER",orderId,old+" -> "+rule[1]);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> parts()throws Exception{return workMapper.findAvailableParts();}

    @Transactional(rollbackFor = Exception.class)
    public void createPartRequest(final long engineerId,final long orderId,final String reason,final String[] partIds,final String[] quantities)throws Exception{
        if(reason.isEmpty()||partIds==null||quantities==null||partIds.length!=quantities.length)throw new IllegalArgumentException("请填写申请原因和至少一项配件");
        if(workMapper.findPartRequestOrderId(engineerId,orderId)==null)throw new IllegalStateException("当前工单不可申请配件");
        if(workMapper.countPendingPartRequests(orderId)>0)throw new IllegalStateException("该工单已有待审核配件申请");
        String requestNo="PR"+System.currentTimeMillis(); Map<String,Object> request=new LinkedHashMap<>(); request.put("requestNo",requestNo);request.put("orderId",orderId);request.put("engineerId",engineerId);request.put("reason",reason);
        workMapper.insertPartRequest(request); long requestId=((Number)request.get("requestId")).longValue(); int added=0;
        for(int i=0;i<partIds.length;i++){long partId=Long.parseLong(partIds[i]);int quantity=Integer.parseInt(quantities[i]);if(partId>0&&quantity>0){workMapper.insertPartRequestItem(requestId,partId,quantity);added++;}}
        if(added==0)throw new IllegalArgumentException("申请数量必须大于0");
        for(Long managerId:workMapper.findWarehouseManagerIds())workMapper.insertPartReviewNotification(managerId,requestNo,requestId);
        workMapper.insertOperationLog(engineerId,"CREATE_PART_REQUEST","PART_REQUEST",requestId,reason);
    }

    private boolean isConstraintViolation(Throwable error){Throwable current=error;while(current!=null){String text=String.valueOf(current.getMessage()).toLowerCase();if(text.contains("unique")||text.contains("duplicate")||text.contains("constraint"))return true;current=current.getCause();}return false;}
}
