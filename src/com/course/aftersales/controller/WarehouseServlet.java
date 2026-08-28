package com.course.aftersales.controller;

import com.course.aftersales.service.WarehouseService;
import com.course.aftersales.util.Web;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;

@WebServlet(urlPatterns={"/warehouse/requests","/warehouse/review","/warehouse/issue","/warehouse/release","/warehouse/return","/warehouse/complete","/warehouse/inventory","/warehouse/stock"})
public class WarehouseServlet extends HttpServlet{
    private final WarehouseService service=new WarehouseService();
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{try{Web.requireRole(req,"WAREHOUSE");if(req.getServletPath().equals("/warehouse/requests")){put(req,service.requestData());Web.view(req,resp,"warehouse/requests");}else if(req.getServletPath().equals("/warehouse/inventory")){req.setAttribute("inventory",service.inventory());req.setAttribute("flows",service.flows());Web.view(req,resp,"warehouse/inventory");}else resp.sendError(404);}catch(Exception e){fail(req,resp,e);}}
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{try{Web.requireRole(req,"WAREHOUSE");long uid=Web.user(req).getId(),id=Web.id(req,"id");String p=req.getServletPath();
        if(p.equals("/warehouse/review"))service.review(uid,id,"approve".equals(Web.text(req,"decision")),Web.text(req,"comment"));
        else if(p.equals("/warehouse/issue"))service.issue(uid,id);
        else if(p.equals("/warehouse/release"))service.release(uid,id,Web.text(req,"reason"));
        else if(p.equals("/warehouse/return"))service.returnPart(uid,Web.id(req,"itemId"),Web.number(req,"quantity",0),Web.text(req,"reason"));
        else if(p.equals("/warehouse/complete"))service.complete(uid,id);
        else if(p.equals("/warehouse/stock")){service.stock(uid,Web.id(req,"partId"),Web.number(req,"quantity",0),Web.text(req,"type"),Web.text(req,"reason"));Web.redirect(req,resp,"/warehouse/inventory","库存及流水已更新");return;}
        else{resp.sendError(404);return;}Web.redirect(req,resp,"/warehouse/requests","配件申请处理成功");
    }catch(Exception e){fail(req,resp,e);}}
    private void put(HttpServletRequest r,Map<String,Object>d){for(Map.Entry<String,Object>e:d.entrySet())r.setAttribute(e.getKey(),e.getValue());}
    private void fail(HttpServletRequest req,HttpServletResponse resp,Exception e)throws ServletException,IOException{Throwable c=e;while(c.getCause()!=null)c=c.getCause();req.setAttribute("error",c.getMessage());req.setAttribute("exception",e);Web.view(req,resp,"error");}
}
