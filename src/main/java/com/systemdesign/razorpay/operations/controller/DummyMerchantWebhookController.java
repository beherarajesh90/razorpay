package com.systemdesign.razorpay.operations.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class DummyMerchantWebhookController {

    @PostMapping
    public ResponseEntity<Void> handleSuccess(@RequestBody Map<String, Object> payload){
        log.info("Received request to handle successful Webhook: {}", payload);
        return ResponseEntity.noContent().build();
    }
}
