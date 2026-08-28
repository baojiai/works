package com.course.aftersales.controller;

import com.course.aftersales.service.CustomerService;
import com.course.aftersales.util.Web;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.Date;
import java.util.Map;

@WebServlet(urlPatterns={"/customer/request","/customer/candidates","/customer/book","/appointments","/appointment/cancel","/orders","/order/detail","/order/accept","/order/review"})
public class CustomerServlet extends HttpServlet {
    private final CustomerService service=new CustomerService();
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        try{
            String path=req.getServletPath();
            if(path.equals("/customer/request")){ Web.requireRole(req,"CUSTOMER"); putForm(req); Web.view(req,resp,"customer/request"); }
            else if(path.equals("/customer/candidates")){ Web.requireRole(req,"CUSTOMER"); long id=Web.id(req,"requestId"); req.setAttribute("repairRequest",service.request(Web.user(req).getId(),id)); req.setAttribute("candidates",service.candidates(Web.user(req).getId(),id,Web.text(req,"sort"))); Web.view(req,resp,"customer/candidates"); }
            else if(path.equals("/appointments")){ req.setAttribute("appointments",service.appointments(Web.user(req))); Web.view(req,resp,"appointments"); }
            else if(path.equals("/orders")){ req.setAttribute("orders",service.orders(Web.user(req))); Web.view(req,resp,"orders"); }
            else if(path.equals("/order/detail")){ Map<String,Object> detail=service.orderDetail(Web.user(req),Web.id(req,"id")); for(Map.Entry<String,Object> e:detail.entrySet())req.setAttribute(e.getKey(),e.getValue()); Web.view(req,resp,"order-detail"); }
            else resp.sendError(404);
        }catch(Exception e){ handle(req,resp,e); }
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        try{
            String path=req.getServletPath();
            if(path.equals("/customer/request")){
                Web.requireRole(req,"CUSTOMER");
                String date=Web.text(req,"expectedDate"); Long slot=Web.id(req,"slotId")>0?Web.id(req,"slotId"):null;
                String query=Web.text(req,"problemQuery");
                String description=Web.text(req,"description");
                if(!query.isEmpty()) description="用户搜索问题："+query+"\n补充描述："+description;
                long id=service.createRequest(Web.user(req).getId(),Web.id(req,"deviceId"),Web.id(req,"faultId"),Web.id(req,"areaId"),description,Web.text(req,"address"),Web.text(req,"phone"),date.isEmpty()?null:Date.valueOf(date),slot);
                Web.redirect(req,resp,"/customer/candidates?requestId="+id,"报修需求已保存，请自主选择工程师和时段");
            }else if(path.equals("/customer/book")){
                Web.requireRole(req,"CUSTOMER"); long id=service.book(Web.user(req).getId(),Web.id(req,"requestId"),Web.id(req,"engineerId"),Web.id(req,"scheduleId"),Web.id(req,"replacesId"));
                Web.redirect(req,resp,"/appointments","预约成功，编号已生成（ID "+id+"）");
            }else if(path.equals("/appointment/cancel")){
                Web.requireRole(req,"CUSTOMER"); service.cancel(Web.user(req).getId(),Web.id(req,"id"),Web.text(req,"reason")); Web.redirect(req,resp,"/appointments","预约已取消，原时段已释放");
            }else if(path.equals("/order/accept")){
                Web.requireRole(req,"CUSTOMER"); service.accept(Web.user(req).getId(),Web.id(req,"id"),"PASSED".equals(Web.text(req,"result")),Web.text(req,"comment")); Web.redirect(req,resp,"/order/detail?id="+Web.id(req,"id"),"验收结果已提交");
            }else if(path.equals("/order/review")){
                Web.requireRole(req,"CUSTOMER"); service.review(Web.user(req).getId(),Web.id(req,"id"),Web.number(req,"rating",0),Web.text(req,"content")); Web.redirect(req,resp,"/order/detail?id="+Web.id(req,"id"),"评价已提交");
            }else resp.sendError(404);
        }catch(Exception e){ handle(req,resp,e); }
    }
    private void putForm(HttpServletRequest req)throws Exception{ for(Map.Entry<String,? extends Object> e:service.formData().entrySet())req.setAttribute(e.getKey(),e.getValue()); }
    private void handle(HttpServletRequest req,HttpServletResponse resp,Exception e)throws ServletException,IOException{
        Throwable cause=e; while(cause.getCause()!=null)cause=cause.getCause(); req.setAttribute("error",cause.getMessage());
        if(req.getServletPath().equals("/customer/request")){try{putForm(req);}catch(Exception ignored){} Web.view(req,resp,"customer/request");}
        else{req.setAttribute("exception",e);Web.view(req,resp,"error");}
    }
}
