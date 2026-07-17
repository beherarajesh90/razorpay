package com.systemdesign.razorpay.payment.service;

import com.systemdesign.razorpay.payment.dto.request.CreateOrderRequest;
import com.systemdesign.razorpay.payment.dto.response.OrderResponse;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public interface OrderService {
    OrderResponse create(UUID merchantId, @Valid CreateOrderRequest request);
}
