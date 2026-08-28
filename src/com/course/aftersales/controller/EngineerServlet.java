package com.course.aftersales.controller;

import com.course.aftersales.service.EngineerService;
import com.course.aftersales.util.Web;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Date;
import java.util.Map;

@WebServlet(urlPatterns={"/engineer/profile","/engineer/schedule","/engineer/schedule/close","/engineer/appointment/cancel","/engineer/order/action","/engineer/order/record","/engineer/part/request"})
public class EngineerServlet extends HttpServlet{
    private final EngineerService service=new EngineerService();
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        try{Web.requireRole(req,"ENGINEER");String p=req.getServletPath();if(p.equals("/engineer/profile")){put(req,service.profileData(Web.user(req).getId()));Web.view(req,resp,"engineer/profile");}else if(p.equals("/engineer/schedule")){put(req,service.scheduleData(Web.user(req).getId()));Web.view(req,resp,"engineer/schedule");}else if(p.equals("/engineer/part/request")){req.setAttribute("orderId",Web.id(req,"orderId"));req.setAttribute("parts",service.parts());Web.view(req,resp,"engineer/part-request");}else resp.sendError(404);}catch(Exception e){fail(req,resp,e);}
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        try{Web.requireRole(req,"ENGINEER");long uid=Web.user(req).getId();String p=req.getServletPath();
            if(p.equals("/engineer/profile")){service.updateProfile(uid,Web.text(req,"phone"),Web.text(req,"bio"),req.getParameterValues("faultId"),req.getParameterValues("areaId"));Web.redirect(req,resp,"/engineer/profile","档案与服务能力已更新");}
            else if(p.equals("/engineer/schedule")){String d=Web.text(req,"serviceDate");service.addSchedule(uid,d.isEmpty()?null:Date.valueOf(d),Web.id(req,"slotId"));Web.redirect(req,resp,"/engineer/schedule","排班已发布");}
            else if(p.equals("/engineer/schedule/close")){service.closeSchedule(uid,Web.id(req,"id"));Web.redirect(req,resp,"/engineer/schedule","空闲时段已关闭");}
            else if(p.equals("/engineer/appointment/cancel")){service.abnormalCancel(uid,Web.id(req,"id"),Web.text(req,"reason"));Web.redirect(req,resp,"/appointments","异常取消已记录，客户已收到改约通知");}
            else if(p.equals("/engineer/order/action")){service.transition(uid,Web.id(req,"id"),Web.text(req,"action"));Web.redirect(req,resp,"/order/detail?id="+Web.id(req,"id"),"工单状态已更新");}
            else if(p.equals("/engineer/order/record")){double h;try{h=Double.parseDouble(Web.text(req,"hours"));}catch(Exception x){h=-1;}service.saveRecord(uid,Web.id(req,"id"),Web.text(req,"diagnosis"),Web.text(req,"repairAction"),h,Web.text(req,"remark"));Web.redirect(req,resp,"/order/detail?id="+Web.id(req,"id"),"维修记录已保存");}
            else if(p.equals("/engineer/part/request")){service.createPartRequest(uid,Web.id(req,"orderId"),Web.text(req,"reason"),req.getParameterValues("partId"),req.getParameterValues("quantity"));Web.redirect(req,resp,"/order/detail?id="+Web.id(req,"orderId"),"配件申请已提交仓库审核");}
            else resp.sendError(404);
        }catch(Exception e){fail(req,resp,e);}
    }
    private void put(HttpServletRequest r,Map<String,Object>d){for(Map.Entry<String,Object>e:d.entrySet())r.setAttribute(e.getKey(),e.getValue());}
    private void fail(HttpServletRequest req,HttpServletResponse resp,Exception e)throws ServletException,IOException{Throwable c=e;while(c.getCause()!=null)c=c.getCause();req.setAttribute("error",c.getMessage());req.setAttribute("exception",e);Web.view(req,resp,"error");}
}
