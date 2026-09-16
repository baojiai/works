package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

public interface DashboardMapper {
    long countCustomerActiveAppointments(@Param("userId") long userId);

    long countCustomerPendingAcceptance(@Param("userId") long userId);

    long countCustomerCompletedOrders(@Param("userId") long userId);

    long countEngineerCustomerAppointments(@Param("userId") long userId);

    long countEngineerActiveOrders(@Param("userId") long userId);

    long countEngineerAvailableSchedules(@Param("userId") long userId);

    long countPendingPartRequests();

    long countApprovedPartRequests();

    long countInventoryWarnings();

    long countActiveUsers();

    long countPendingReschedules();

    long countActiveOrders();
}
