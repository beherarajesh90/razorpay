package com.systemdesign.razorpay.merchant.controller;

import com.systemdesign.razorpay.common.dto.FindOrCreateCustomerRequest;
import com.systemdesign.razorpay.merchant.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Service-to-service customer lookup used by payment-service when creating orders.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/customers")
public class InternalCustomerController {

    private final CustomerService customerService;

    @PostMapping("/find-or-create")
    public UUID findOrCreate(@RequestBody FindOrCreateCustomerRequest request) {
        return customerService.findOrCreate(request.merchantId(), request.email(), request.name(), request.phone());
    }
}
