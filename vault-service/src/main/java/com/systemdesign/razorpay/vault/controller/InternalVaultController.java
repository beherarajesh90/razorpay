package com.systemdesign.razorpay.vault.controller;

import com.systemdesign.razorpay.common.dto.PaymentProcessorResponse;
import com.systemdesign.razorpay.common.dto.VaultChargeRequest;
import com.systemdesign.razorpay.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service charge used by payment-service. Never exposed through the gateway.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/vault")
public class InternalVaultController {

    private final VaultService vaultService;

    @PostMapping("/charge")
    public PaymentProcessorResponse charge(@RequestBody VaultChargeRequest request) {
        return vaultService.charge(request.paymentId(), request.token(), request.amount(), request.methodDetails());
    }
}
