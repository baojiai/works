package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface NotificationMapper {
    List<Map<String,Object>> findByUserId(@Param("userId") long userId);

    long countUnread(@Param("userId") long userId);

    int markRead(@Param("userId") long userId, @Param("notificationId") long notificationId);

    int markAllRead(@Param("userId") long userId);
}
