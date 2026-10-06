package com.systemdesign.razorpay.payment.mapper;

import com.systemdesign.razorpay.payment.dto.response.RefundResponse;
import com.systemdesign.razorpay.payment.entity.Refund;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RefundMapper {
    @Mapping(target = "refundId", source = "id")
    @Mapping(target = "paymentId", source = "payment.id")
    RefundResponse toResponse(Refund refund);
}
