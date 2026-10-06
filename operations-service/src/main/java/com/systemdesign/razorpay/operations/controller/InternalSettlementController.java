package com.systemdesign.razorpay.operations.controller;

import com.systemdesign.razorpay.operations.settlement.SettlementEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runs the settlement batch on demand. Same work as the nightly cron. Not routed by the gateway.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/settlements")
public class InternalSettlementController {

    private final SettlementEngine settlementEngine;

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void run() {
        settlementEngine.run();
    }
}
