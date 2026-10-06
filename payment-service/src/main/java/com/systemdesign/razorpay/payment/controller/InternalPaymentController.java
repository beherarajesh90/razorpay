package com.systemdesign.razorpay.payment.controller;

import com.systemdesign.razorpay.common.dto.PaymentSettlementView;
import com.systemdesign.razorpay.payment.api.PaymentLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Service-to-service read used by operations-service for nightly settlement. Not routed by the gateway.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/payments")
public class InternalPaymentController {

    private final PaymentLookupService paymentLookupService;

    @GetMapping("/captured")
    public List<PaymentSettlementView> capturedPayments(@RequestParam("merchantId") UUID merchantId) {
        return paymentLookupService.findUnsettledCapturedPayments(merchantId).stream()
                .map(p -> new PaymentSettlementView(p.getId(), p.getMerchantId(), p.getAmount()))
                .toList();
    }
}
