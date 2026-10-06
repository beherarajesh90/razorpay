package com.systemdesign.razorpay.vault.service;

import com.systemdesign.razorpay.common.entity.Money;
import com.systemdesign.razorpay.common.dto.PaymentProcessorResponse;
import com.systemdesign.razorpay.vault.dto.request.TokenizeRequest;
import com.systemdesign.razorpay.vault.dto.response.TokenizeResponse;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public interface VaultService {
    TokenizeResponse tokenize(TokenizeRequest request, UUID merchantId);
    PaymentProcessorResponse charge(UUID paymentId, String token, Money amount, Map<String, Object> methodDetails);
}
