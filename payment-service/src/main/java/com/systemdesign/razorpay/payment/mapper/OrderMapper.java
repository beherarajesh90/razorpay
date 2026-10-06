package com.systemdesign.razorpay.payment.mapper;

import com.systemdesign.razorpay.payment.dto.response.OrderResponse;
import com.systemdesign.razorpay.payment.entity.OrderRecord;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    OrderResponse toResponse(OrderRecord orderRecord);
}
