package com.systemdesign.razorpay.payment.service.impl;

import com.systemdesign.razorpay.common.enums.OrderStatus;
import com.systemdesign.razorpay.common.exception.DuplicateResourceException;
import com.systemdesign.razorpay.payment.dto.request.CreateOrderRequest;
import com.systemdesign.razorpay.payment.dto.response.OrderResponse;
import com.systemdesign.razorpay.payment.entity.OrderRecord;
import com.systemdesign.razorpay.payment.repository.OrderRepository;
import com.systemdesign.razorpay.payment.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Value("${payment.order.default-order-expiry-minutes: 10}")
    private int defaultOrderExpiryMinutes;

    @Override
    @Transactional
    public OrderResponse create(UUID merchantId, CreateOrderRequest request) {
        if(request.receipt()!=null && orderRepository.existsByMerchantIdAndReceipt(merchantId, request.receipt())){
            throw new DuplicateResourceException("ORDER_RECEIPT_DUPLICATE","order with receipt already exists: "+request.receipt());
        }

        OrderRecord order = OrderRecord.builder()
                .merchantId(merchantId)
                .receipt(request.receipt())
                .amount(request.amount())
                .notes(request.notes())
                .orderStatus(OrderStatus.CREATED)
                .expiresAt(request.expiresAt()!=null ? request.expiresAt() : LocalDateTime.now().plusMinutes(defaultOrderExpiryMinutes))
                .build();
        order = orderRepository.save(order);

        // TODO: publish kafka event amount order creation

        return new OrderResponse(
                order.getId(), order.getMerchantId(), order.getReceipt(), order.getAmount(),
                order.getOrderStatus(), order.getAttempts(), order.getNotes(), order.getExpiresAt(), null
        );
    }
}
