package com.systemdesign.razorpay.merchant.controller;

import com.systemdesign.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.systemdesign.razorpay.merchant.dto.response.MerchantResponse;
import com.systemdesign.razorpay.merchant.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public ResponseEntity<MerchantResponse> signup(@Valid @RequestBody MerchantSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.signup(request));
    }
}
