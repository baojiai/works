package com.course.aftersales.service;

import com.course.aftersales.mapper.AdminAuditMapper;
import com.course.aftersales.mapper.AdminBasicDataMapper;
import com.course.aftersales.mapper.AdminConfigMapper;
import com.course.aftersales.mapper.AdminStatisticsMapper;
import com.course.aftersales.mapper.AdminUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final AdminUserMapper userMapper;
    private final AdminAuditMapper auditMapper;
    private final AdminConfigMapper configMapper;
    private final AdminBasicDataMapper basicDataMapper;
    private final AdminStatisticsMapper statisticsMapper;
    private final EngineerApplicationService applicationService;

    @Autowired
    public AdminService(AdminUserMapper userMapper,
                        AdminAuditMapper auditMapper,
                        AdminConfigMapper configMapper,
                        AdminBasicDataMapper basicDataMapper,
                        AdminStatisticsMapper statisticsMapper,
                        EngineerApplicationService applicationService) {
        this.userMapper = userMapper;
        this.auditMapper = auditMapper;
        this.configMapper = configMapper;
        this.basicDataMapper = basicDataMapper;
        this.statisticsMapper = statisticsMapper;
        this.applicationService = applicationService;
    }

    @Transactional(readOnly = true)
    public Map<String,Object> data() throws Exception {
        Map<String,Object> data = new HashMap<>();
        data.put("users", userMapper.findUsers());
        data.put("configs", configMapper.findConfigs());
        data.put("devices", basicDataMapper.findDevices());
        data.put("faults", basicDataMapper.findFaults());
        data.put("areas", basicDataMapper.findAreas());
        data.put("slots", basicDataMapper.findSlots());
        data.put("exceptions", statisticsMapper.findExceptions());
        data.put("logs", statisticsMapper.findRecentLogs());
        data.put("stats", statisticsMapper.findStatistics());
        data.put("applications", applicationService.applications());
        data.put("applicationSkills", applicationService.applicationSkills());
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public void setUserStatus(long admin,long userId,String status) throws Exception {
        if(!Arrays.asList("ACTIVE","DISABLED").contains(status)) throw new IllegalArgumentException("用户状态无效");
        if(userId==admin&&"DISABLED".equals(status)) throw new IllegalArgumentException("不能停用当前管理员账号");
        userMapper.updateUserStatus(userId,status);
        userMapper.insertOperationLog(admin,"SET_USER_STATUS","USER",userId,status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setQualification(long admin,long engineerId,String status) throws Exception {
        if(!Arrays.asList("APPROVED","EXPIRED","PENDING").contains(status)) throw new IllegalArgumentException("资质状态无效");
        auditMapper.updateQualification(engineerId,status);
        auditMapper.insertOperationLog(admin,"SET_QUALIFICATION","ENGINEER",engineerId,status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(long admin,String key,String value) throws Exception {
        if(key.isEmpty()||value.isEmpty()) throw new IllegalArgumentException("配置键和值不能为空");
        if(configMapper.updateConfig(admin,key,value)==0) configMapper.insertConfig(admin,key,value,"管理员新增参数");
        configMapper.insertOperationLog(admin,"UPDATE_CONFIG","CONFIG",0,key+"="+value);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addBasic(long admin,String kind,String name,long parent,String start,String end) throws Exception {
        if(name.isEmpty()) throw new IllegalArgumentException("名称不能为空");
        Map<String,Object> data=new LinkedHashMap<>();
        data.put("name",name);
        long id;
        if("device".equals(kind)) {
            basicDataMapper.insertDevice(data);
        } else if("fault".equals(kind)) {
            if(parent<=0) throw new IllegalArgumentException("故障类别必须选择设备类别");
            data.put("parentId",parent);
            basicDataMapper.insertFault(data);
        } else if("area".equals(kind)) {
            basicDataMapper.insertArea(data);
        } else if("slot".equals(kind)) {
            if(start.isEmpty()||end.isEmpty()) throw new IllegalArgumentException("标准时段需要开始和结束时间");
            data.put("startTime",normalizeTime(start));
            data.put("endTime",normalizeTime(end));
            basicDataMapper.insertSlot(data);
        } else {
            throw new IllegalArgumentException("基础数据类型无效");
        }
        id=((Number)data.get("generatedId")).longValue();
        basicDataMapper.insertOperationLog(admin,"ADD_BASIC_DATA",kind.toUpperCase(),id,name);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(long admin,long applicationId,boolean approve,String comment) throws Exception {
        applicationService.review(admin,applicationId,approve,comment);
    }

    @Transactional(rollbackFor = Exception.class)
    public int expireReschedules(long admin) throws Exception {
        int hours=config(configMapper,"RESCHEDULE_HOURS",24);
        List<Map<String,Object>> rows=statisticsMapper.findPendingReschedulesBefore(LocalDateTime.now().minusHours(hours));
        int count=0;
        for(Map<String,Object> row:rows) {
            long appointmentId=((Number)row.get("appointment_id")).longValue();
            statisticsMapper.markAppointmentExpired(appointmentId);
            statisticsMapper.cancelOrder(row.get("order_id"));
            statisticsMapper.insertRescheduleTimeoutChange(appointmentId,admin);
            statisticsMapper.insertNotification(((Number)row.get("customer_id")).longValue(),"RESCHEDULE_EXPIRED",
                    "待改约预约已过期","预约因超过改约时限已关闭","APPOINTMENT",appointmentId);
            count++;
        }
        statisticsMapper.insertOperationLog(admin,"EXPIRE_RESCHEDULES","SYSTEM",0,"处理数量："+count);
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    public int runSla(long admin) throws Exception {
        int count=0;
        int reminder=config(configMapper,"APPOINTMENT_REMINDER_HOURS",12);
        LocalDateTime now=LocalDateTime.now();
        LocalDateTime until=now.plusHours(reminder);
        for(Map<String,Object> appointment:statisticsMapper.findUpcomingAppointments(until.toLocalDate())) {
            LocalDateTime appointmentTime=LocalDateTime.of(toDate(appointment.get("service_date")),toTime(appointment.get("start_time")));
            if(!appointmentTime.isBefore(now)&&!appointmentTime.isAfter(until)) {
                long appointmentId=((Number)appointment.get("appointment_id")).longValue();
                count+=notifyOnce(statisticsMapper,((Number)appointment.get("customer_id")).longValue(),"APPOINTMENT_REMINDER",
                        "预约即将开始","请准备按约接受维修服务","APPOINTMENT",appointmentId);
                count+=notifyOnce(statisticsMapper,((Number)appointment.get("engineer_id")).longValue(),"APPOINTMENT_REMINDER",
                        "预约即将开始","请按预约时间提供维修服务","APPOINTMENT",appointmentId);
            }
        }
        int orderHours=config(configMapper,"ORDER_SLA_HOURS",48);
        for(Map<String,Object> order:statisticsMapper.findOverdueOrders(now.minusHours(orderHours))) {
            count+=notifyOnce(statisticsMapper,((Number)order.get("engineer_id")).longValue(),"ORDER_OVERDUE",
                    "维修工单处理超时","请尽快更新维修进度","ORDER",((Number)order.get("order_id")).longValue());
        }
        int partHours=config(configMapper,"PART_REVIEW_SLA_HOURS",8);
        List<Long> managers=statisticsMapper.findWarehouseManagerIds();
        for(Long requestId:statisticsMapper.findOverduePartRequestIds(now.minusHours(partHours))) {
            for(Long managerId:managers) {
                count+=notifyOnce(statisticsMapper,managerId,"PART_REVIEW_OVERDUE","配件申请审核超时",
                        "请尽快处理待审核申请","PART_REQUEST",requestId);
            }
        }
        statisticsMapper.insertOperationLog(admin,"RUN_SLA","SYSTEM",0,"生成通知："+count);
        return count;
    }

    private int config(AdminConfigMapper mapper,String key,int fallback) {
        String value=mapper.findConfigValue(key);
        try { return value==null?fallback:Integer.parseInt(value); }
        catch(Exception e) { return fallback; }
    }

    private int notifyOnce(AdminStatisticsMapper mapper,long user,String type,String title,String content,String business,long id) {
        if(mapper.findExistingNotification(user,type,business,id)!=null) return 0;
        mapper.insertNotification(user,type,title,content,business,id);
        return 1;
    }

    private String normalizeTime(String value) {
        return value.length()==5?value+":00":value;
    }

    private LocalDate toDate(Object value) {
        return value instanceof LocalDate?(LocalDate)value:LocalDate.parse(String.valueOf(value));
    }

    private LocalTime toTime(Object value) {
        return value instanceof LocalTime?(LocalTime)value:LocalTime.parse(String.valueOf(value));
    }
}
