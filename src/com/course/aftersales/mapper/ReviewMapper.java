package com.course.aftersales.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.Map;

public interface ReviewMapper {
    Map<String,Object> findOrderForAcceptance(@Param("customerId") long customerId, @Param("orderId") long orderId);
    int insertAcceptance(Map<String,Object> acceptance);
    int markOrderCompleted(@Param("orderId") long orderId);
    int markOrderRework(@Param("orderId") long orderId);
    int insertAcceptanceStatusLog(@Param("orderId") long orderId, @Param("customerId") long customerId,
                                  @Param("nextStatus") String nextStatus, @Param("comment") String comment);
    int markAppointmentFulfilled(@Param("appointmentId") Object appointmentId);
    int insertRework(@Param("orderId") long orderId, @Param("acceptanceId") long acceptanceId, @Param("comment") String comment);
    int insertReworkNotification(@Param("engineerId") Object engineerId, @Param("comment") String comment, @Param("orderId") long orderId);
    Map<String,Object> findOrderForReview(@Param("customerId") long customerId, @Param("orderId") long orderId);
    int insertReview(@Param("orderId") long orderId, @Param("customerId") long customerId,
                     @Param("engineerId") Object engineerId, @Param("rating") int rating, @Param("content") String content);
    Map<String,Object> findEngineerReviewStats(@Param("engineerId") long engineerId);
    Map<String,Object> findEngineerFulfillmentStats(@Param("engineerId") long engineerId);
    int updateEngineerStats(@Param("engineerId") long engineerId,
                            @Param("completedCount") Object completedCount,
                            @Param("averageRating") Object averageRating,
                            @Param("reviewCount") Object reviewCount,
                            @Param("fulfillmentRate") double fulfillmentRate);
    int insertOperationLog(@Param("userId") long userId, @Param("operation") String operation,
                           @Param("businessType") String businessType, @Param("businessId") long businessId,
                           @Param("description") String description);
}
