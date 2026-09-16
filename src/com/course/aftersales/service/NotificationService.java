package com.course.aftersales.service;

import com.course.aftersales.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class NotificationService{
    private final NotificationMapper mapper;

    @Autowired
    public NotificationService(NotificationMapper mapper) { this.mapper = mapper; }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> list(long user)throws Exception{return mapper.findByUserId(user);}
    @Transactional(readOnly = true)
    public long unreadCount(long user)throws Exception{return mapper.countUnread(user);}
    @Transactional(rollbackFor = Exception.class)
    public void read(long user,long id)throws Exception{if(mapper.markRead(user,id)!=1)throw new SecurityException("无权操作该通知");}
    @Transactional(rollbackFor = Exception.class)
    public void readAll(long user)throws Exception{mapper.markAllRead(user);}
}
