package com.course.aftersales.controller;

import com.course.aftersales.service.AdminService;
import com.course.aftersales.util.Web;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;

@WebServlet(urlPatterns={"/admin","/admin/user","/admin/qualification","/admin/application","/admin/config","/admin/basic","/admin/expire","/admin/sla"})
public class AdminServlet extends HttpServlet{
    private final AdminService service=new AdminService();
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{try{Web.requireRole(req,"ADMIN");put(req,service.data());Web.view(req,resp,"admin/index");}catch(Exception e){fail(req,resp,e);}}
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{try{Web.requireRole(req,"ADMIN");long uid=Web.user(req).getId();String p=req.getServletPath(),msg;
        if(p.equals("/admin/user")){service.setUserStatus(uid,Web.id(req,"id"),Web.text(req,"status"));msg="用户状态已更新";}
        else if(p.equals("/admin/qualification")){service.setQualification(uid,Web.id(req,"id"),Web.text(req,"status"));msg="工程师资质已更新";}
        else if(p.equals("/admin/application")){service.reviewApplication(uid,Web.id(req,"id"),"approve".equals(Web.text(req,"decision")),Web.text(req,"comment"));msg="工程师认证申请已处理";}
        else if(p.equals("/admin/config")){service.updateConfig(uid,Web.text(req,"key"),Web.text(req,"value"));msg="系统参数已保存";}
        else if(p.equals("/admin/basic")){service.addBasic(uid,Web.text(req,"kind"),Web.text(req,"name"),Web.id(req,"parentId"),Web.text(req,"startTime"),Web.text(req,"endTime"));msg="基础数据已新增";}
        else if(p.equals("/admin/expire"))msg="已处理 "+service.expireReschedules(uid)+" 条超时改约";
        else if(p.equals("/admin/sla"))msg="已生成 "+service.runSla(uid)+" 条新提醒";
        else{resp.sendError(404);return;}Web.redirect(req,resp,"/admin",msg);
    }catch(Exception e){fail(req,resp,e);}}
    private void put(HttpServletRequest r,Map<String,Object>d){for(Map.Entry<String,Object>e:d.entrySet())r.setAttribute(e.getKey(),e.getValue());}
    private void fail(HttpServletRequest req,HttpServletResponse resp,Exception e)throws ServletException,IOException{Throwable c=e;while(c.getCause()!=null)c=c.getCause();req.setAttribute("error",c.getMessage());req.setAttribute("exception",e);Web.view(req,resp,"error");}
}
