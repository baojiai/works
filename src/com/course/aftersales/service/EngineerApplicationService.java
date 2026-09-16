package com.course.aftersales.service;

import com.course.aftersales.mapper.EngineerApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EngineerApplicationService {
    private final EngineerApplicationMapper mapper;

    @Autowired
    public EngineerApplicationService(EngineerApplicationMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Map<String,Object> formData(long userId) throws Exception {
        Map<String,Object> data = new HashMap<>();
        data.put("areas", mapper.findActiveAreas());
        data.put("faults", mapper.findActiveFaults());
        data.put("latest", mapper.findLatestApplication(userId));
        return data;
    }

    @Transactional(rollbackFor = Exception.class)
    public void submit(long userId, String realName, String idCardNo, String phone, long areaId, int years,
                       String certificateNo, String skillDescription, String materialDescription, String[] faultIds) throws Exception {
        if (realName.isEmpty() || idCardNo.isEmpty() || phone.isEmpty() || areaId <= 0 || skillDescription.isEmpty() || materialDescription.isEmpty()) throw new IllegalArgumentException("请完整填写认证资料");
        if (!phone.matches("^1[3-9]\\d{9}$")) throw new IllegalArgumentException("请输入正确的 11 位手机号");
        if (years < 0) throw new IllegalArgumentException("从业年限不能为负数");
        if (faultIds == null || faultIds.length == 0) throw new IllegalArgumentException("请至少选择一类可维修故障");
        if (mapper.findPendingApplicationId(userId) != null) throw new IllegalStateException("已有待审核申请，请等待管理员处理");
        if (mapper.findActiveAreaId(areaId) == null) throw new IllegalArgumentException("服务区域无效");
        Map<String,Object> application = new LinkedHashMap<>();
        application.put("userId", userId); application.put("realName", realName); application.put("idCardNo", idCardNo);
        application.put("phone", phone); application.put("areaId", areaId); application.put("years", years);
        application.put("certificateNo", certificateNo); application.put("skillDescription", skillDescription);
        application.put("materialDescription", materialDescription);
        mapper.insertApplication(application);
        long applicationId = ((Number) application.get("applicationId")).longValue();
        for (String faultId : faultIds) {
            long id = Long.parseLong(faultId);
            if (mapper.findActiveFaultId(id) == null) throw new IllegalArgumentException("维修技能选择无效");
            mapper.insertApplicationSkill(applicationId, id);
        }
        for (Long adminId : mapper.findActiveAdminIds()) mapper.insertApplyNotification(adminId, realName + " 提交了工程师认证资料", applicationId);
        mapper.insertOperationLog(userId, "SUBMIT_ENGINEER_APPLICATION", "ENGINEER_APPLICATION", applicationId, "提交工程师认证");
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> applications() throws Exception { return mapper.findApplications(); }
    @Transactional(readOnly = true)
    public List<Map<String,Object>> applications(Object ignored) throws Exception { return applications(); }
    @Transactional(readOnly = true)
    public List<Map<String,Object>> applicationSkills() throws Exception { return mapper.findApplicationSkills(); }
    @Transactional(readOnly = true)
    public List<Map<String,Object>> applicationSkills(Object ignored) throws Exception { return applicationSkills(); }

    @Transactional(rollbackFor = Exception.class)
    public void review(long adminId, long applicationId, boolean approve, String comment) throws Exception {
        if (!approve && comment.trim().isEmpty()) throw new IllegalArgumentException("驳回申请时请填写审核意见");
        Map<String,Object> application = mapper.findPendingApplication(applicationId);
        if (application == null) throw new IllegalStateException("该申请不存在或已经审核");
        long userId = ((Number) application.get("user_id")).longValue();
        mapper.updateApplicationReview(applicationId, approve ? "APPROVED" : "REJECTED", adminId, comment);
        if (approve) {
            mapper.updateUserAsEngineer(userId, application.get("real_name"), application.get("phone"));
            if (mapper.findEngineerProfileId(userId) == null) mapper.insertEngineerProfile(userId, application.get("skill_description"));
            else mapper.updateEngineerProfile(userId, application.get("skill_description"));
            mapper.deleteEngineerSkills(userId);
            for (Long faultId : mapper.findApplicationFaultIds(applicationId)) mapper.insertEngineerSkill(userId, faultId);
            mapper.deleteEngineerAreas(userId);
            mapper.insertEngineerArea(userId, application.get("service_area_id"));
        }
        mapper.insertReviewNotification(userId, approve ? "工程师认证已通过" : "工程师认证未通过", approve ? "请重新登录后进入工程师版发布可约时段" : comment, applicationId);
        mapper.insertOperationLog(adminId, approve ? "APPROVE_ENGINEER_APPLICATION" : "REJECT_ENGINEER_APPLICATION", "ENGINEER_APPLICATION", applicationId, comment);
    }

}
