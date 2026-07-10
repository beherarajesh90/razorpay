package com.systemdesign.razorpay.merchant.service;

import com.systemdesign.razorpay.merchant.dto.request.CreateApiKeyRequest;
import com.systemdesign.razorpay.merchant.dto.response.ApiKeyCreateResponse;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public interface ApiKeyService {
    ApiKeyCreateResponse create(UUID merchantId, CreateApiKeyRequest request);
}
