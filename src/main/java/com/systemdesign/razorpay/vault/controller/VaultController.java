package com.systemdesign.razorpay.vault.controller;

import com.systemdesign.razorpay.merchant.security.MerchantContext;
import com.systemdesign.razorpay.vault.dto.request.TokenizeRequest;
import com.systemdesign.razorpay.vault.dto.response.TokenizeResponse;
import com.systemdesign.razorpay.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/vault")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;
    private final MerchantContext merchantContext;

    @PostMapping("/tokenize")
    public ResponseEntity<TokenizeResponse> tokenize(@RequestBody TokenizeRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vaultService.tokenize(request, merchantContext.getMerchantId()));
    }
}
