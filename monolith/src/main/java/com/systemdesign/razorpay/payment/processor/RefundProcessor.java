package com.systemdesign.razorpay.payment.processor;

import com.systemdesign.razorpay.common.enums.RefundStatus;
import com.systemdesign.razorpay.payment.repository.RefundRepository;
import com.systemdesign.razorpay.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundProcessor {
    private final RefundRepository refundRepository;
    private final RefundService refundService;

    @Scheduled(fixedDelay = 5000)
    public void processPendingRefunds() {
        refundRepository.findTop100ByStatusInOrderByCreatedAtAsc(
                        List.of(RefundStatus.PENDING, RefundStatus.PROCESSING))
                .forEach(refund -> {
                    try {
                        refundService.processRefund(refund.getId());
                    } catch (RuntimeException ex) {
                        log.error("Refund processing failed, refundId={}", refund.getId(), ex);
                    }
                });
    }
}
