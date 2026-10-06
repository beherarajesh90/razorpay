package com.systemdesign.razorpay.merchant.controller;

import com.systemdesign.razorpay.merchant.dto.request.WebhookConfigRequest;
import com.systemdesign.razorpay.merchant.dto.response.WebhookConfigResponse;
import com.systemdesign.razorpay.common.context.MerchantContext;
import com.systemdesign.razorpay.merchant.service.WebhookConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/merchants/webhooks")
public class WebhookConfigController {

    private final WebhookConfigService webhookConfigService;
    private final MerchantContext merchantContext;

    @PostMapping
    public ResponseEntity<WebhookConfigResponse> create(@Valid @RequestBody WebhookConfigRequest webhookConfigRequest){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(webhookConfigService.create(merchantContext.getMerchantId(), webhookConfigRequest));
    }

    @GetMapping
    public ResponseEntity<List<WebhookConfigResponse>> list(){
        return ResponseEntity.ok(webhookConfigService.list(merchantContext.getMerchantId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookConfigResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(webhookConfigService.getById(merchantContext.getMerchantId(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebhookConfigResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody WebhookConfigRequest request) {
        return ResponseEntity.ok(webhookConfigService.update(merchantContext.getMerchantId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        webhookConfigService.delete(merchantContext.getMerchantId(), id);
        return ResponseEntity.noContent().build();
    }

}
