package com.course.aftersales.mapper;

import java.util.List;
import java.util.Map;

public interface WarehousePartMapper {
    List<Map<String,Object>> findRequestItemsWithPartDetails();
    List<Map<String,Object>> findInventoryParts();
}
