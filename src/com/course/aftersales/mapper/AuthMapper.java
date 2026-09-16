package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.Map;

public interface AuthMapper {
    Map<String,Object> findUserByAccount(@Param("account") String account);

    int insertLoginLog(@Param("userId") long userId);

    Long findExistingUserId(@Param("phone") String phone);

    int insertUser(Map<String,Object> user);

    int insertCustomerProfile(@Param("userId") long userId);

    int insertRegisterLog(@Param("userId") long userId);
}
