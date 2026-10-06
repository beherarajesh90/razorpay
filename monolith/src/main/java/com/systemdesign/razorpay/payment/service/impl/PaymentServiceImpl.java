package com.systemdesign.razorpay.payment.service.impl;

import com.systemdesign.razorpay.common.enums.EventAggregateType;
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
import com.systemdesign.razorpay.payment.outbox.OutboxEventPublisher;
import com.systemdesign.razorpay.payment.repository.OrderRepository;
import com.systemdesign.razorpay.payment.repository.PaymentRepository;
import com.systemdesign.razorpay.payment.service.PaymentService;
import com.systemdesign.razorpay.payment.statemachine.PaymentTransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
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
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    @Transactional
    public PaymentResponse initiate(UUID merchantId, PaymentInitRequest request) {
//        OrderRecord order = orderRepository.findByIdAndMerchantId(request.orderId(), merchantId)
//                .orElseThrow(() -> new ResourceNotFoundException("Order", request.orderId()));

        OrderRecord order = orderRepository.findByIdAndMerchantIdForUpdate(request.orderId(), merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.orderId()));

        if(order.getOrderStatus() != OrderStatus.CREATED && order.getOrderStatus() != OrderStatus.ATTEMPTED){
            throw new BusinessRuleViolationException("ORDER_NOT_PAYABLE", "Order cannot accept payment when order status is: "+order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.ATTEMPTED);
        order.setAttempts(order.getAttempts() + 1);

        Payment payment = Payment.builder()
                .merchantId(merchantId)
                .order(order)
                .amount(order.getAmount())
                .status(PaymentStatus.CREATED)
                .method(request.method())
                .idempotencyKey(UUID.randomUUID().toString())   // TODO: generate idempotency key
                .methodDetails(request.methodDetails())
                .build();

        payment = paymentRepository.save(payment);

        PaymentRequest paymentRequest = new PaymentRequest(payment.getId(),
                order.getId(), merchantId, order.getAmount(), request.method(), request.methodDetails());

        paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_ATTEMPT);
        PaymentResult paymentResult = paymentGatewayRouter.initiate(paymentRequest);
        switch (paymentResult){
            case PaymentResult.Pending pending -> payment.setProcessorReference(pending.registrationRef());
            case PaymentResult.Failure failure-> {
                payment.setStatus(paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_FAIL));
                payment.setErrorCode(failure.errorCode());
                payment.setErrorDescription(failure.errorDescription());
            }
            case PaymentResult.Success success -> {
                log.warn("Invalid state");
                return null;
            }
        }

        payment = paymentRepository.save(payment);
        orderRepository.save(order);

        outboxEventPublisher.publish(EventAggregateType.PAYMENT, payment.getId(), "PAYMENT_CREATED",
                Map.of("orderId", order.getId().toString(),
                        "paymentId", payment.getId().toString(),
                        "merchantId", merchantId.toString(),
                        "paymentStatus", payment.getStatus().name(),
                        "amountUnits", order.getAmount().getAmountUnits(),
                        "amountCurrency", order.getAmount().getCurrency(),
                        "paymentMethod", payment.getMethod()
                )
        );

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse capture(UUID merchantId, UUID paymentId) {
//        Payment payment = paymentRepository.findByIdAndMerchantId(paymentId, merchantId)
//                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        Payment payment = paymentRepository.findByIdAndMerchantIdForUpdate(paymentId, merchantId)
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

        outboxEventPublisher.publish(EventAggregateType.PAYMENT, payment.getId(), "PAYMENT_STATUS_CHANGED",
                Map.of("orderId", payment.getOrder().getId().toString(),
                        "paymentId", payment.getId().toString(),
                        "merchantId", merchantId.toString(),
                        "paymentStatus", payment.getStatus().name(),
                        "amountUnits", payment.getAmount().getAmountUnits(),
                        "amountCurrency", payment.getAmount().getCurrency(),
                        "paymentMethod", payment.getMethod()
                )
        );

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public void resolveAuthorization(UUID paymentId, boolean approve, String bankRef, String errorCode, String errorDescription) {
//        Payment payment = paymentRepository.findById(paymentId).orElseThrow(
//                () -> new ResourceNotFoundException("Payment", paymentId)
//        );

        Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElseThrow(
                () -> new ResourceNotFoundException("Payment", paymentId)
        );

        if(payment.getStatus() != PaymentStatus.AUTHORIZING){
            log.warn("Payment is not in authorizing state, paymentId: {}, status: {}", paymentId, payment.getStatus());
            return;
        }

        OrderRecord orderRecord = payment.getOrder();

        if(approve){
            paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_SUCCESS);
            payment.setBankReference(bankRef);
            payment.setAuthorizedAt(LocalDateTime.now());

            //auto-capture
            paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_REQUEST);
            PaymentResult captureResult = paymentGatewayRouter.capture(payment.getMethod(), paymentId);

            if(captureResult instanceof PaymentResult.Success success){
                paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_SUCCESS);
                payment.setCapturedAt(LocalDateTime.now());
                orderRecord.setOrderStatus(OrderStatus.PAID);
            } else if(captureResult instanceof PaymentResult.Failure failure){
                paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_FAIL);
                payment.setErrorCode(failure.errorCode());
                payment.setErrorDescription(failure.errorDescription());
            }
        } else{
            paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_FAIL);
            payment.setErrorCode(errorCode);
            payment.setErrorDescription(errorDescription);
        }

        paymentRepository.save(payment);
        orderRepository.save(orderRecord);

        outboxEventPublisher.publish(EventAggregateType.PAYMENT, payment.getId(), "PAYMENT_STATUS_CHANGED",
                Map.of("orderId", payment.getOrder().getId().toString(),
                        "paymentId", payment.getId().toString(),
                        "merchantId", payment.getMerchantId().toString(),
                        "paymentStatus", payment.getStatus().name(),
                        "amountUnits", payment.getAmount().getAmountUnits(),
                        "amountCurrency", payment.getAmount().getCurrency(),
                        "paymentMethod", payment.getMethod()
                )
        );
    }
}
