package com.systemdesign.razorpay.payment.service.impl;

import com.systemdesign.razorpay.common.enums.OrderStatus;
import com.systemdesign.razorpay.common.enums.PaymentEvent;
import com.systemdesign.razorpay.common.enums.PaymentStatus;
import com.systemdesign.razorpay.common.exception.BusinessRuleViolationException;
import com.systemdesign.razorpay.common.exception.ResourceNotFoundException;
import com.systemdesign.razorpay.payment.dto.request.PaymentInitRequest;
import com.systemdesign.razorpay.payment.dto.response.PaymentResponse;
import com.systemdesign.razorpay.payment.entity.OrderRecord;
import com.systemdesign.razorpay.payment.entity.Payment;
import com.systemdesign.razorpay.payment.gateway.PaymentGatewayRouter;
import com.systemdesign.razorpay.payment.gateway.dto.request.PaymentRequest;
import com.systemdesign.razorpay.payment.gateway.dto.respose.PaymentResult;
import com.systemdesign.razorpay.payment.mapper.PaymentMapper;
import com.systemdesign.razorpay.payment.repository.OrderRepository;
import com.systemdesign.razorpay.payment.repository.PaymentRepository;
import com.systemdesign.razorpay.payment.service.PaymentService;
import com.systemdesign.razorpay.payment.statemachine.PaymentTransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayRouter paymentGatewayRouter;
    private final PaymentMapper paymentMapper;
    private final PaymentTransitionService paymentTransitionService;

    @Override
    public PaymentResponse initiate(UUID merchantId, PaymentInitRequest request) {
        OrderRecord order = orderRepository.findByIdAndMerchantId(request.orderId(), merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.orderId()));

        if(order.getOrderStatus() != OrderStatus.CREATED && order.getOrderStatus() != OrderStatus.ATTEMPTED){
            throw new BusinessRuleViolationException("ORDER_NOT_PAYABLE", "Order cannot accept payment when order status is: "+order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.ATTEMPTED);
        order.setAttempts(order.getAttempts() + 1);

        Payment payment = Payment.builder()
                .merchantId(merchantId)
                .order(order)
                .money(order.getAmount())
                .status(PaymentStatus.CREATED)
                .method(request.method())
                .methodDetails(request.methodDetails())
                .build();

        payment = paymentRepository.save(payment);

        PaymentRequest paymentRequest = new PaymentRequest(payment.getId(),
                order.getId(), merchantId, order.getAmount(), request.method(), request.methodDetails());

        PaymentResult paymentResult = paymentGatewayRouter.initiate(paymentRequest);
        switch (paymentResult){
            case PaymentResult.Pending pending -> payment.setProcessorReference(pending.registrationRef());
            case PaymentResult.Failure failure-> {
                payment.setStatus(paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_FAIL));
                payment.setErrorCode(failure.errorCode());
                payment.setErrorDescription(failure.errorDescription());
            }
            case PaymentResult.Success success -> {

            }
        }

        payment = paymentRepository.save(payment);
        orderRepository.save(order);

        // TODO: send an outbox event (kafka event)

        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse capture(UUID merchantId, UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndMerchantId(paymentId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_REQUEST);

        PaymentResult paymentResult = paymentGatewayRouter.capture(payment.getMethod(), paymentId);

        if (paymentResult instanceof PaymentResult.Success success){
            paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_SUCCESS);
            payment.setCapturedAt(LocalDateTime.now());
            log.info("Payment captured successfully, paymentId: {}", paymentId);
        } else if (paymentResult instanceof PaymentResult.Failure failure){
            paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_FAIL);
            payment.setErrorCode(failure.errorCode());
            payment.setErrorDescription(failure.errorDescription());
            log.error("Payment capture failed, paymentId: {}", paymentId);
        }

        payment = paymentRepository.save(payment);

//        TODO: send an outbox (kafka event)

        return paymentMapper.toResponse(payment);
    }
}
