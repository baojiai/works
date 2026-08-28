package com.course.aftersales.service;

import com.course.aftersales.repository.Database;
import java.sql.Connection;
import java.util.*;

public class NotificationService{
    public List<Map<String,Object>> list(long user)throws Exception{try(Connection c=Database.open()){return Database.query(c,"SELECT * FROM notification WHERE receiver_id=? ORDER BY is_read,created_at DESC",user);}}
    public void read(long user,long id)throws Exception{try(Connection c=Database.open()){if(Database.update(c,"UPDATE notification SET is_read=TRUE WHERE notification_id=? AND receiver_id=?",id,user)!=1)throw new SecurityException("无权操作该通知");}}
    public void readAll(long user)throws Exception{try(Connection c=Database.open()){Database.update(c,"UPDATE notification SET is_read=TRUE WHERE receiver_id=?",user);}}
}

