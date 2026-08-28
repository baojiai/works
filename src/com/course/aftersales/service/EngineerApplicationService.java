package com.course.aftersales.service;

import com.course.aftersales.repository.Database;
import java.sql.Connection;
import java.util.*;

public class EngineerApplicationService {
    public Map<String,Object> formData(long userId) throws Exception {
        Map<String,Object> data = new HashMap<>();
        try (Connection c = Database.open()) {
            data.put("areas", Database.query(c, "SELECT service_area_id,name FROM service_area WHERE status='ACTIVE' ORDER BY name"));
            data.put("faults", Database.query(c, "SELECT f.fault_type_id,f.name,d.name device_name FROM fault_type f JOIN device_type d ON d.device_type_id=f.device_type_id WHERE f.status='ACTIVE' ORDER BY d.name,f.name"));
            data.put("latest", Database.one(c, "SELECT ea.*,sa.name area_name,u.display_name reviewer_name FROM engineer_application ea JOIN service_area sa ON sa.service_area_id=ea.service_area_id LEFT JOIN system_user u ON u.user_id=ea.reviewer_id WHERE ea.user_id=? ORDER BY ea.created_at DESC LIMIT 1", userId));
        }
        return data;
    }

    public void submit(long userId, String realName, String idCardNo, String phone, long areaId, int years,
                       String certificateNo, String skillDescription, String materialDescription, String[] faultIds) throws Exception {
        if (realName.isEmpty() || idCardNo.isEmpty() || phone.isEmpty() || areaId <= 0 || skillDescription.isEmpty() || materialDescription.isEmpty()) {
            throw new IllegalArgumentException("请完整填写认证资料");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) throw new IllegalArgumentException("请输入正确的 11 位手机号");
        if (years < 0) throw new IllegalArgumentException("从业年限不能为负数");
        if (faultIds == null || faultIds.length == 0) throw new IllegalArgumentException("请至少选择一类可维修故障");
        Database.tx(c -> {
            Map<String,Object> pending = Database.one(c, "SELECT application_id FROM engineer_application WHERE user_id=? AND status='PENDING'", userId);
            if (pending != null) throw new IllegalStateException("已有待审核申请，请等待管理员处理");
            Map<String,Object> area = Database.one(c, "SELECT service_area_id FROM service_area WHERE service_area_id=? AND status='ACTIVE'", areaId);
            if (area == null) throw new IllegalArgumentException("服务区域无效");
            long appId = Database.insert(c,
                    "INSERT INTO engineer_application(user_id,real_name,id_card_no,phone,service_area_id,experience_years,certificate_no,skill_description,material_description,status) VALUES(?,?,?,?,?,?,?,?,?,'PENDING')",
                    userId, realName, idCardNo, phone, areaId, years, certificateNo, skillDescription, materialDescription);
            for (String faultId : faultIds) {
                long id = Long.parseLong(faultId);
                Map<String,Object> fault = Database.one(c, "SELECT fault_type_id FROM fault_type WHERE fault_type_id=? AND status='ACTIVE'", id);
                if (fault == null) throw new IllegalArgumentException("维修技能选择无效");
                Database.update(c, "INSERT INTO engineer_application_skill(application_id,fault_type_id) VALUES(?,?)", appId, id);
            }
            List<Map<String,Object>> admins = Database.query(c, "SELECT user_id FROM system_user WHERE role_type='ADMIN' AND status='ACTIVE'");
            for (Map<String,Object> admin : admins) {
                Database.update(c, "INSERT INTO notification(receiver_id,notification_type,title,content,related_business_type,related_business_id) VALUES(?,'ENGINEER_APPLY','新的工程师认证待审核',?,'ENGINEER_APPLICATION',?)",
                        admin.get("user_id"), realName + " 提交了工程师认证资料", appId);
            }
            log(c, userId, "SUBMIT_ENGINEER_APPLICATION", "ENGINEER_APPLICATION", appId, "提交工程师认证");
        });
    }

    public List<Map<String,Object>> applications(Connection c) throws Exception {
        return Database.query(c,
                "SELECT ea.*,su.display_name user_name,sa.name area_name,ru.display_name reviewer_name " +
                "FROM engineer_application ea JOIN system_user su ON su.user_id=ea.user_id " +
                "JOIN service_area sa ON sa.service_area_id=ea.service_area_id LEFT JOIN system_user ru ON ru.user_id=ea.reviewer_id " +
                "ORDER BY CASE ea.status WHEN 'PENDING' THEN 0 ELSE 1 END, ea.created_at DESC");
    }

    public List<Map<String,Object>> applicationSkills(Connection c) throws Exception {
        return Database.query(c,
                "SELECT eas.application_id,ft.name fault_name,dt.name device_name FROM engineer_application_skill eas " +
                "JOIN fault_type ft ON ft.fault_type_id=eas.fault_type_id JOIN device_type dt ON dt.device_type_id=ft.device_type_id ORDER BY eas.application_id,dt.name,ft.name");
    }

    public void review(long adminId, long applicationId, boolean approve, String comment) throws Exception {
        if (!approve && comment.trim().isEmpty()) throw new IllegalArgumentException("驳回申请时请填写审核意见");
        Database.tx(c -> {
            Map<String,Object> app = Database.one(c, "SELECT * FROM engineer_application WHERE application_id=? AND status='PENDING'", applicationId);
            if (app == null) throw new IllegalStateException("该申请不存在或已经审核");
            long userId = ((Number) app.get("user_id")).longValue();
            Database.update(c, "UPDATE engineer_application SET status=?,reviewer_id=?,review_comment=?,reviewed_at=CURRENT_TIMESTAMP WHERE application_id=?",
                    approve ? "APPROVED" : "REJECTED", adminId, comment, applicationId);
            if (approve) {
                Database.update(c, "UPDATE system_user SET role_type='ENGINEER',display_name=?,phone=? WHERE user_id=?",
                        app.get("real_name"), app.get("phone"), userId);
                Map<String,Object> profile = Database.one(c, "SELECT engineer_id FROM engineer_profile WHERE engineer_id=?", userId);
                if (profile == null) {
                    Database.update(c, "INSERT INTO engineer_profile(engineer_id,bio,qualification_status,employment_status) VALUES(?,?,'APPROVED','ACTIVE')",
                            userId, app.get("skill_description"));
                } else {
                    Database.update(c, "UPDATE engineer_profile SET bio=?,qualification_status='APPROVED',employment_status='ACTIVE' WHERE engineer_id=?",
                            app.get("skill_description"), userId);
                }
                Database.update(c, "DELETE FROM engineer_skill WHERE engineer_id=?", userId);
                for (Map<String,Object> skill : Database.query(c, "SELECT fault_type_id FROM engineer_application_skill WHERE application_id=?", applicationId)) {
                    Database.update(c, "INSERT INTO engineer_skill(engineer_id,fault_type_id,proficiency_level) VALUES(?,?,3)", userId, skill.get("fault_type_id"));
                }
                Database.update(c, "DELETE FROM engineer_service_area WHERE engineer_id=?", userId);
                Database.update(c, "INSERT INTO engineer_service_area(engineer_id,service_area_id) VALUES(?,?)", userId, app.get("service_area_id"));
            }
            Database.update(c, "INSERT INTO notification(receiver_id,notification_type,title,content,related_business_type,related_business_id) VALUES(?,'ENGINEER_APPLY_RESULT',?,?,'ENGINEER_APPLICATION',?)",
                    userId, approve ? "工程师认证已通过" : "工程师认证未通过", approve ? "请重新登录后进入工程师版发布可约时段" : comment, applicationId);
            log(c, adminId, approve ? "APPROVE_ENGINEER_APPLICATION" : "REJECT_ENGINEER_APPLICATION", "ENGINEER_APPLICATION", applicationId, comment);
        });
    }

    private void log(Connection c, long user, String op, String type, long id, String text) throws Exception {
        Database.update(c, "INSERT INTO operation_log(user_id,operation_type,business_type,business_id,description) VALUES(?,?,?,?,?)", user, op, type, id, text);
    }
}
