package com.course.aftersales.service;

import com.course.aftersales.mapper.WarehouseInventoryMapper;
import com.course.aftersales.mapper.WarehousePartMapper;
import com.course.aftersales.mapper.WarehouseRequestMapper;
import com.course.aftersales.mapper.WarehouseReturnMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WarehouseService {
    private final WarehousePartMapper partMapper;
    private final WarehouseInventoryMapper inventoryMapper;
    private final WarehouseRequestMapper requestMapper;
    private final WarehouseReturnMapper returnMapper;

    @Autowired
    public WarehouseService(WarehousePartMapper partMapper,
                            WarehouseInventoryMapper inventoryMapper,
                            WarehouseRequestMapper requestMapper,
                            WarehouseReturnMapper returnMapper) {
        this.partMapper = partMapper;
        this.inventoryMapper = inventoryMapper;
        this.requestMapper = requestMapper;
        this.returnMapper = returnMapper;
    }

    @Transactional(readOnly = true)
    public Map<String,Object> requestData() throws Exception {
        Map<String,Object> data=new HashMap<>();
        data.put("requests",requestMapper.findRequests());
        data.put("items",partMapper.findRequestItemsWithPartDetails());
        return data;
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> inventory() throws Exception {
        return partMapper.findInventoryParts();
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> flows() throws Exception {
        return inventoryMapper.findRecentFlows();
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(long operator,long requestId,boolean approve,String comment) throws Exception {
        Map<String,Object> request=requestMapper.findPendingRequest(requestId);
        if(request==null) throw new IllegalStateException("该申请已处理或不存在");
        List<Map<String,Object>> items=requestMapper.findRequestItems(requestId);
        if(items.isEmpty()) throw new IllegalStateException("申请无有效明细");
        if(approve) {
            for(Map<String,Object> item:items) {
                int quantity=((Number)item.get("request_quantity")).intValue();
                long partId=((Number)item.get("part_id")).longValue();
                if(inventoryMapper.lockInventory(partId,quantity)!=1) throw new IllegalStateException("配件ID "+partId+" 可用库存不足");
                inventoryMapper.insertRequestFlow(partId,request.get("order_id"),requestId,"LOCK",quantity,"申请审核通过，锁定库存",operator);
            }
            requestMapper.approveRequest(requestId,operator,comment);
        } else {
            requestMapper.rejectRequest(requestId,operator,comment);
        }
        requestMapper.insertNotification(((Number)request.get("engineer_id")).longValue(),"PART_RESULT",
                approve?"配件申请已通过":"配件申请被驳回",comment,"PART_REQUEST",requestId);
        requestMapper.insertOperationLog(operator,approve?"APPROVE_PART_REQUEST":"REJECT_PART_REQUEST","PART_REQUEST",requestId,comment);
    }

    @Transactional(rollbackFor = Exception.class)
    public void issue(long operator,long requestId) throws Exception {
        Map<String,Object> request=requestMapper.findApprovedRequest(requestId);
        if(request==null) throw new IllegalStateException("仅审核通过且未出库的申请可出库");
        for(Map<String,Object> item:requestMapper.findRequestItems(requestId)) {
            int requested=((Number)item.get("request_quantity")).intValue();
            int issued=((Number)item.get("issued_quantity")).intValue();
            int quantity=requested-issued;
            long partId=((Number)item.get("part_id")).longValue();
            if(quantity<=0) continue;
            if(inventoryMapper.issueInventory(partId,quantity)!=1) throw new IllegalStateException("锁定库存不一致，出库已回滚");
            requestMapper.incrementItemIssued(item.get("item_id"),quantity);
            inventoryMapper.insertRequestFlow(partId,request.get("order_id"),requestId,"OUT",quantity,"审核通过配件实际出库",operator);
        }
        requestMapper.markRequestIssued(requestId);
        requestMapper.insertNotification(((Number)request.get("engineer_id")).longValue(),"PART_ISSUED",
                "配件已出库","请领取配件并继续维修","PART_REQUEST",requestId);
        requestMapper.insertOperationLog(operator,"ISSUE_PART","PART_REQUEST",requestId,"配件出库");
    }

    @Transactional(rollbackFor = Exception.class)
    public void release(long operator,long requestId,String reason) throws Exception {
        if(reason.isEmpty()) throw new IllegalArgumentException("请填写释放原因");
        Map<String,Object> request=requestMapper.findApprovedRequest(requestId);
        if(request==null) throw new IllegalStateException("仅已锁定且未出库申请可取消释放");
        for(Map<String,Object> item:requestMapper.findRequestItems(requestId)) {
            int quantity=((Number)item.get("request_quantity")).intValue();
            long partId=((Number)item.get("part_id")).longValue();
            if(inventoryMapper.unlockInventory(partId,quantity)!=1) throw new IllegalStateException("锁定库存不足，释放回滚");
            inventoryMapper.insertRequestFlow(partId,request.get("order_id"),requestId,"UNLOCK",quantity,reason,operator);
        }
        requestMapper.cancelRequest(requestId,reason);
        requestMapper.insertNotification(((Number)request.get("engineer_id")).longValue(),"PART_RESULT",
                "配件申请已取消",reason,"PART_REQUEST",requestId);
        requestMapper.insertOperationLog(operator,"RELEASE_PART","PART_REQUEST",requestId,reason);
    }

    @Transactional(rollbackFor = Exception.class)
    public void returnPart(long operator,long itemId,int quantity,String reason) throws Exception {
        if(quantity<=0||reason.isEmpty()) throw new IllegalArgumentException("退回数量必须大于0并填写原因");
        Map<String,Object> item=returnMapper.findReturnableItem(itemId);
        if(item==null) throw new IllegalStateException("该配件明细不可退回");
        int maximum=((Number)item.get("issued_quantity")).intValue()-((Number)item.get("return_quantity")).intValue();
        if(quantity>maximum) throw new IllegalStateException("退回数量超过尚未退回的已出库数量");
        returnMapper.restoreInventory(item.get("part_id"),quantity);
        returnMapper.incrementItemReturn(itemId,quantity);
        returnMapper.insertReturnFlow(item.get("part_id"),item.get("order_id"),item.get("part_request_id"),quantity,reason,operator);
        returnMapper.insertOperationLog(operator,"RETURN_PART","PART_REQUEST",((Number)item.get("part_request_id")).longValue(),reason);
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(long operator,long requestId) throws Exception {
        if(requestMapper.markRequestCompleted(requestId)!=1) throw new IllegalStateException("仅已出库申请可确认完成");
        requestMapper.insertOperationLog(operator,"COMPLETE_PART_REQUEST","PART_REQUEST",requestId,"已核对出库与退回，申请完成");
    }

    @Transactional(rollbackFor = Exception.class)
    public void stock(long operator,long partId,int quantity,String type,String reason) throws Exception {
        if(reason.isEmpty()||quantity==0) throw new IllegalArgumentException("数量不能为0且必须填写原因");
        String flow="RESTOCK".equals(type)?"IN":"ADJUST";
        if("RESTOCK".equals(type)&&quantity<0) throw new IllegalArgumentException("补充入库数量必须大于0");
        if(inventoryMapper.adjustInventory(partId,quantity)!=1) throw new IllegalStateException("调整后库存不能为负且总量不能小于锁定量");
        inventoryMapper.insertStockFlow(partId,flow,Math.abs(quantity),reason,operator);
        requestMapper.insertOperationLog(operator,flow,"PART",partId,reason);
    }
}
