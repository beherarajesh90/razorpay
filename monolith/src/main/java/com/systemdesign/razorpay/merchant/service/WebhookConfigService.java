package com.systemdesign.razorpay.merchant.service;

import com.systemdesign.razorpay.merchant.dto.request.WebhookConfigRequest;
import com.systemdesign.razorpay.merchant.dto.response.WebhookConfigResponse;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface WebhookConfigService {
    WebhookConfigResponse create(UUID merchantId, WebhookConfigRequest request);

    List<WebhookConfigResponse> list(UUID merchantId);

    WebhookConfigResponse getById(UUID merchantId, UUID configId);

    WebhookConfigResponse update(UUID merchantId, UUID configId, WebhookConfigRequest request);

    void delete(UUID merchantId, UUID configId);
}
