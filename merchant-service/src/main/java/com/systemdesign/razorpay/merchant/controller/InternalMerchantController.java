package com.systemdesign.razorpay.merchant.controller;

import com.systemdesign.razorpay.common.dto.SettlementBankDetails;
import com.systemdesign.razorpay.common.dto.WebhookTarget;
import com.systemdesign.razorpay.merchant.api.MerchantLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Service-to-service lookups used by operations-service and payment-service.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/merchants")
public class InternalMerchantController {

    private final MerchantLookupService merchantLookupService;

    @GetMapping("/{merchantId}/webhook-targets")
    public List<WebhookTarget> webhookTargets(@PathVariable UUID merchantId, @RequestParam String eventType) {
        return merchantLookupService.getActiveConfigsForEvent(merchantId, eventType);
    }

    @GetMapping("/active-ids")
    public List<UUID> activeMerchantIds() {
        return merchantLookupService.listActiveMerchantIds();
    }

    @GetMapping("/{merchantId}/settlement-bank-details")
    public SettlementBankDetails settlementBankDetails(@PathVariable UUID merchantId) {
        return merchantLookupService.getSettlementBankDetails(merchantId);
    }
}
