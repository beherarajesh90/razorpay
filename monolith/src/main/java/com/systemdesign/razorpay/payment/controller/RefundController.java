package com.systemdesign.razorpay.payment.controller;

import com.systemdesign.razorpay.merchant.security.MerchantContext;
import com.systemdesign.razorpay.payment.dto.request.RefundRequest;
import com.systemdesign.razorpay.payment.dto.response.RefundResponse;
import com.systemdesign.razorpay.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final MerchantContext merchantContext;

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<RefundResponse> initiateRefund(@PathVariable UUID paymentId,
                                                         @RequestHeader("X-Idempotency-Key") String idempotencyKey,
                                                         @Valid @RequestBody RefundRequest refundRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(refundService.initiateRefund(paymentId, merchantContext.getMerchantId(), refundRequest, idempotencyKey));
    }

    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<RefundResponse> getRefund(@PathVariable UUID refundId){
        return ResponseEntity.ok(refundService.getRefund(refundId, merchantContext.getMerchantId()));
    }

}
