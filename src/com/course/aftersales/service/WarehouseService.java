package com.course.aftersales.service;

import com.course.aftersales.repository.Database;
import java.sql.Connection;
import java.util.*;

public class WarehouseService {
    public Map<String,Object> requestData()throws Exception{
        Map<String,Object>d=new HashMap<>();try(Connection c=Database.open()){
            d.put("requests",Database.query(c,"SELECT pr.*,u.display_name engineer_name,ro.order_no FROM part_request pr JOIN system_user u ON u.user_id=pr.engineer_id JOIN repair_order ro ON ro.order_id=pr.order_id ORDER BY CASE pr.status WHEN 'PENDING' THEN 0 WHEN 'APPROVED' THEN 1 ELSE 2 END,pr.created_at DESC"));
            d.put("items",Database.query(c,"SELECT i.*,p.part_code,p.name part_name,p.model,p.unit,inv.available_quantity,inv.locked_quantity FROM part_request_item i JOIN part p ON p.part_id=i.part_id JOIN part_inventory inv ON inv.part_id=i.part_id ORDER BY i.part_request_id,i.item_id"));
        }return d;
    }
    public List<Map<String,Object>> inventory()throws Exception{try(Connection c=Database.open()){return Database.query(c,"SELECT p.*,i.total_quantity,i.available_quantity,i.locked_quantity,i.issued_quantity,i.warning_threshold FROM part p JOIN part_inventory i ON i.part_id=p.part_id ORDER BY p.name");}}
    public List<Map<String,Object>> flows()throws Exception{try(Connection c=Database.open()){return Database.query(c,"SELECT f.*,p.name part_name,u.display_name operator_name FROM inventory_flow f JOIN part p ON p.part_id=f.part_id JOIN system_user u ON u.user_id=f.operator_id ORDER BY f.created_at DESC LIMIT 100");}}
    public void review(final long operator,final long requestId,final boolean approve,final String comment)throws Exception{
        Database.tx(c->{
            Map<String,Object> request=Database.one(c,"SELECT * FROM part_request WHERE part_request_id=? AND status='PENDING'",requestId);if(request==null)throw new IllegalStateException("该申请已处理或不存在");
            List<Map<String,Object>> items=Database.query(c,"SELECT * FROM part_request_item WHERE part_request_id=?",requestId);if(items.isEmpty())throw new IllegalStateException("申请无有效明细");
            if(approve){
                for(Map<String,Object> item:items){int qty=((Number)item.get("request_quantity")).intValue();long part=((Number)item.get("part_id")).longValue();int changed=Database.update(c,"UPDATE part_inventory SET available_quantity=available_quantity-?,locked_quantity=locked_quantity+? WHERE part_id=? AND available_quantity>=?",qty,qty,part,qty);if(changed!=1)throw new IllegalStateException("配件ID "+part+" 可用库存不足");Database.update(c,"INSERT INTO inventory_flow(part_id,order_id,part_request_id,flow_type,quantity,reason,operator_id) VALUES(?,?,?,'LOCK',?,?,?)",part,request.get("order_id"),requestId,qty,"申请审核通过，锁定库存",operator);}
                Database.update(c,"UPDATE part_request SET status='APPROVED',reviewer_id=?,reviewed_at=CURRENT_TIMESTAMP,review_comment=? WHERE part_request_id=?",operator,comment,requestId);
            }else Database.update(c,"UPDATE part_request SET status='REJECTED',reviewer_id=?,reviewed_at=CURRENT_TIMESTAMP,review_comment=? WHERE part_request_id=?",operator,comment,requestId);
            notify(c,((Number)request.get("engineer_id")).longValue(),"PART_RESULT",approve?"配件申请已通过":"配件申请被驳回",comment,"PART_REQUEST",requestId);
            log(c,operator,approve?"APPROVE_PART_REQUEST":"REJECT_PART_REQUEST","PART_REQUEST",requestId,comment);
        });
    }
    public void issue(final long operator,final long requestId)throws Exception{
        Database.tx(c->{
            Map<String,Object> request=Database.one(c,"SELECT * FROM part_request WHERE part_request_id=? AND status='APPROVED'",requestId);if(request==null)throw new IllegalStateException("仅审核通过且未出库的申请可出库");
            List<Map<String,Object>> items=Database.query(c,"SELECT * FROM part_request_item WHERE part_request_id=?",requestId);
            for(Map<String,Object> item:items){int requestQty=((Number)item.get("request_quantity")).intValue();int issued=((Number)item.get("issued_quantity")).intValue();int qty=requestQty-issued;long part=((Number)item.get("part_id")).longValue();if(qty<=0)continue;int changed=Database.update(c,"UPDATE part_inventory SET total_quantity=total_quantity-?,locked_quantity=locked_quantity-?,issued_quantity=issued_quantity+? WHERE part_id=? AND locked_quantity>=?",qty,qty,qty,part,qty);if(changed!=1)throw new IllegalStateException("锁定库存不一致，出库已回滚");Database.update(c,"UPDATE part_request_item SET issued_quantity=issued_quantity+? WHERE item_id=?",qty,item.get("item_id"));Database.update(c,"INSERT INTO inventory_flow(part_id,order_id,part_request_id,flow_type,quantity,reason,operator_id) VALUES(?,?,?,'OUT',?,?,?)",part,request.get("order_id"),requestId,qty,"审核通过配件实际出库",operator);}
            Database.update(c,"UPDATE part_request SET status='ISSUED' WHERE part_request_id=?",requestId);notify(c,((Number)request.get("engineer_id")).longValue(),"PART_ISSUED","配件已出库","请领取配件并继续维修","PART_REQUEST",requestId);log(c,operator,"ISSUE_PART","PART_REQUEST",requestId,"配件出库");
        });
    }
    public void release(final long operator,final long requestId,final String reason)throws Exception{
        if(reason.isEmpty())throw new IllegalArgumentException("请填写释放原因");Database.tx(c->{
            Map<String,Object> request=Database.one(c,"SELECT * FROM part_request WHERE part_request_id=? AND status='APPROVED'",requestId);if(request==null)throw new IllegalStateException("仅已锁定且未出库申请可取消释放");
            for(Map<String,Object> item:Database.query(c,"SELECT * FROM part_request_item WHERE part_request_id=?",requestId)){int qty=((Number)item.get("request_quantity")).intValue();long part=((Number)item.get("part_id")).longValue();if(Database.update(c,"UPDATE part_inventory SET available_quantity=available_quantity+?,locked_quantity=locked_quantity-? WHERE part_id=? AND locked_quantity>=?",qty,qty,part,qty)!=1)throw new IllegalStateException("锁定库存不足，释放回滚");Database.update(c,"INSERT INTO inventory_flow(part_id,order_id,part_request_id,flow_type,quantity,reason,operator_id) VALUES(?,?,?,'UNLOCK',?,?,?)",part,request.get("order_id"),requestId,qty,reason,operator);}
            Database.update(c,"UPDATE part_request SET status='CANCELLED',review_comment=? WHERE part_request_id=?",reason,requestId);notify(c,((Number)request.get("engineer_id")).longValue(),"PART_RESULT","配件申请已取消",reason,"PART_REQUEST",requestId);log(c,operator,"RELEASE_PART","PART_REQUEST",requestId,reason);
        });
    }
    public void returnPart(final long operator,final long itemId,final int qty,final String reason)throws Exception{
        if(qty<=0||reason.isEmpty())throw new IllegalArgumentException("退回数量必须大于0并填写原因");Database.tx(c->{
            Map<String,Object> item=Database.one(c,"SELECT i.*,pr.order_id,pr.part_request_id,pr.status FROM part_request_item i JOIN part_request pr ON pr.part_request_id=i.part_request_id WHERE i.item_id=? AND pr.status IN ('ISSUED','COMPLETED')",itemId);if(item==null)throw new IllegalStateException("该配件明细不可退回");int max=((Number)item.get("issued_quantity")).intValue()-((Number)item.get("return_quantity")).intValue();if(qty>max)throw new IllegalStateException("退回数量超过尚未退回的已出库数量");
            Database.update(c,"UPDATE part_inventory SET total_quantity=total_quantity+?,available_quantity=available_quantity+?,issued_quantity=issued_quantity-? WHERE part_id=?",qty,qty,qty,item.get("part_id"));Database.update(c,"UPDATE part_request_item SET return_quantity=return_quantity+? WHERE item_id=?",qty,itemId);Database.update(c,"INSERT INTO inventory_flow(part_id,order_id,part_request_id,flow_type,quantity,reason,operator_id) VALUES(?,?,?,'RETURN',?,?,?)",item.get("part_id"),item.get("order_id"),item.get("part_request_id"),qty,reason,operator);log(c,operator,"RETURN_PART","PART_REQUEST",((Number)item.get("part_request_id")).longValue(),reason);
        });
    }
    public void complete(long operator,long requestId)throws Exception{try(Connection c=Database.open()){if(Database.update(c,"UPDATE part_request SET status='COMPLETED' WHERE part_request_id=? AND status='ISSUED'",requestId)!=1)throw new IllegalStateException("仅已出库申请可确认完成");log(c,operator,"COMPLETE_PART_REQUEST","PART_REQUEST",requestId,"已核对出库与退回，申请完成");}}
    public void stock(final long operator,final long partId,final int quantity,final String type,final String reason)throws Exception{
        if(reason.isEmpty()||quantity==0)throw new IllegalArgumentException("数量不能为0且必须填写原因");Database.tx(c->{
            String flow="RESTOCK".equals(type)?"IN":"ADJUST";if("RESTOCK".equals(type)&&quantity<0)throw new IllegalArgumentException("补充入库数量必须大于0");
            int changed=Database.update(c,"UPDATE part_inventory SET total_quantity=total_quantity+?,available_quantity=available_quantity+? WHERE part_id=? AND total_quantity+?>=locked_quantity AND available_quantity+?>=0",quantity,quantity,partId,quantity,quantity);if(changed!=1)throw new IllegalStateException("调整后库存不能为负且总量不能小于锁定量");Database.update(c,"INSERT INTO inventory_flow(part_id,flow_type,quantity,reason,operator_id) VALUES(?,?,?,?,?)",partId,flow,Math.abs(quantity),reason,operator);log(c,operator,flow,"PART",partId,reason);
        });
    }
    private void notify(Connection c,long user,String type,String title,String content,String business,long id)throws Exception{Database.update(c,"INSERT INTO notification(receiver_id,notification_type,title,content,related_business_type,related_business_id) VALUES(?,?,?,?,?,?)",user,type,title,content,business,id);}
    private void log(Connection c,long user,String op,String type,long id,String text)throws Exception{Database.update(c,"INSERT INTO operation_log(user_id,operation_type,business_type,business_id,description) VALUES(?,?,?,?,?)",user,op,type,id,text);}
}

