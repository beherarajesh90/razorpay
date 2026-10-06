package com.systemdesign.razorpay.operations.client;

import com.systemdesign.razorpay.common.dto.PaymentSettlementView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "payment-service", path = "/internal/v1/payments")
public interface PaymentServiceClient {

    @GetMapping("/captured")
    List<PaymentSettlementView> findCapturedPayments(@RequestParam("merchantId") UUID merchantId);
}
