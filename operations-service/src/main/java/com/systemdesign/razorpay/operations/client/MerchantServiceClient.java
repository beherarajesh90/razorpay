package com.systemdesign.razorpay.operations.client;

import com.systemdesign.razorpay.common.dto.SettlementBankDetails;
import com.systemdesign.razorpay.common.dto.WebhookTarget;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "merchant-service", path = "/internal/v1/merchants")
public interface MerchantServiceClient {

    @GetMapping("/{merchantId}/webhook-targets")
    List<WebhookTarget> getActiveConfigsForEvent(@PathVariable("merchantId") UUID merchantId,
                                                 @RequestParam("eventType") String eventType);

    @GetMapping("/active-ids")
    List<UUID> listActiveMerchantIds();

    @GetMapping("/{merchantId}/settlement-bank-details")
    SettlementBankDetails getSettlementBankDetails(@PathVariable("merchantId") UUID merchantId);
}
