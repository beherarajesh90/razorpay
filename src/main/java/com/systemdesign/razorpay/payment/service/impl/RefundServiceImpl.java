package com.systemdesign.razorpay.payment.service.impl;

import com.systemdesign.razorpay.common.entity.Money;
import com.systemdesign.razorpay.common.enums.EventAggregateType;
import com.systemdesign.razorpay.common.enums.PaymentStatus;
import com.systemdesign.razorpay.common.enums.RefundEvent;
import com.systemdesign.razorpay.common.enums.RefundStatus;
import com.systemdesign.razorpay.common.exception.BusinessRuleViolationException;
import com.systemdesign.razorpay.common.exception.ResourceNotFoundException;
import com.systemdesign.razorpay.payment.dto.request.RefundRequest;
import com.systemdesign.razorpay.payment.dto.response.RefundResponse;
import com.systemdesign.razorpay.payment.entity.Payment;
import com.systemdesign.razorpay.payment.entity.Refund;
import com.systemdesign.razorpay.payment.gateway.PaymentGatewayRouter;
import com.systemdesign.razorpay.payment.gateway.dto.respose.PaymentResult;
import com.systemdesign.razorpay.payment.mapper.RefundMapper;
import com.systemdesign.razorpay.payment.outbox.OutboxEventPublisher;
import com.systemdesign.razorpay.payment.repository.PaymentRepository;
import com.systemdesign.razorpay.payment.repository.RefundRepository;
import com.systemdesign.razorpay.payment.service.RefundService;
import com.systemdesign.razorpay.payment.statemachine.RefundStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final RefundMapper refundMapper;
    private final RefundStateMachine refundStateMachine;
    private final PaymentGatewayRouter paymentGatewayRouter;
    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    @Transactional
    public RefundResponse initiateRefund(UUID paymentId, UUID merchantId, RefundRequest request, String idempotencyKey) {
        Refund existing = refundRepository.findByMerchantIdAndIdempotencyKey(merchantId, idempotencyKey).orElse(null);
        if (existing != null) return refundMapper.toResponse(existing);

        Payment payment = paymentRepository.findByIdAndMerchantIdForUpdate(paymentId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if (payment.getStatus() != PaymentStatus.CAPTURED
                && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED
                && payment.getStatus() != PaymentStatus.SETTLED) {
            throw new BusinessRuleViolationException("PAYMENT_NOT_REFUNDABLE",
                    "Payment cannot be refunded in status: " + payment.getStatus());
        }

        long reserved = refundRepository.sumReservedAmount(paymentId);
        long amount = request.amount() == null ? payment.getAmount().getAmountUnits() - reserved : request.amount();
        if (amount <= 0 || reserved + amount > payment.getAmount().getAmountUnits()) {
            throw new BusinessRuleViolationException("REFUND_AMOUNT_EXCEEDS_PAYMENT",
                    "Refund amount exceeds the remaining refundable amount");
        }

        Refund refund = refundRepository.save(Refund.builder()
                .payment(payment)
                .merchantId(merchantId)
                .idempotencyKey(idempotencyKey)
                .amount(Money.of(Math.toIntExact(amount), payment.getAmount().getCurrency()))
                .status(RefundStatus.PENDING)
                .notes(request.reason() == null ? null : Map.of("reason", request.reason()))
                .build());
        outboxEventPublisher.publish(EventAggregateType.REFUND, refund.getId(), "REFUND_REQUESTED",
                Map.of("refundId", refund.getId().toString(), "paymentId", paymentId.toString(),
                        "merchantId", merchantId.toString(), "amountUnits", amount,
                        "currency", payment.getAmount().getCurrency()));
        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefund(UUID refundId, UUID merchantId) {
        return refundRepository.findByIdAndMerchantId(refundId, merchantId)
                .map(refundMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", refundId));
    }

    @Override
    @Transactional
    public void processRefund(UUID refundId) {
        Refund refund = refundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", refundId));
        if (refund.getStatus() == RefundStatus.PROCESSED || refund.getStatus() == RefundStatus.FAILED) return;

        refund.setStatus(refundStateMachine.transition(refund.getStatus(), RefundEvent.PROCESS));
        PaymentResult result;
        try {
            result = paymentGatewayRouter.refund(refund.getPayment().getMethod(), refund.getPayment().getId(), refund.getAmount());
        } catch (RuntimeException ex) {
            result = new PaymentResult.Failure("REFUND_PROCESSING_ERROR", ex.getMessage());
        }

        if (result instanceof PaymentResult.Success success) {
            refund.setStatus(refundStateMachine.transition(refund.getStatus(), RefundEvent.SUCCESS));
            refund.setBankReference(success.bankReference());
            refund.setProcessedAt(LocalDateTime.now());
            Payment payment = refund.getPayment();
            long total = refundRepository.sumReservedAmount(payment.getId());
            payment.setStatus(total >= payment.getAmount().getAmountUnits()
                    ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
            payment.setRefundedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        } else if (result instanceof PaymentResult.Failure failure) {
            refund.setStatus(refundStateMachine.transition(refund.getStatus(), RefundEvent.FAILURE));
            refund.setErrorCode(failure.errorCode());
            refund.setErrorDescription(failure.errorDescription());
        }

        refundRepository.save(refund);
        outboxEventPublisher.publish(EventAggregateType.REFUND, refund.getId(), "REFUND_STATUS_CHANGED",
                Map.of("refundId", refund.getId().toString(), "paymentId", paymentId(refund),
                        "merchantId", refund.getMerchantId().toString(), "refundStatus", refund.getStatus().name()));
    }

    private String paymentId(Refund refund) {
        return refund.getPayment().getId().toString();
    }
}
