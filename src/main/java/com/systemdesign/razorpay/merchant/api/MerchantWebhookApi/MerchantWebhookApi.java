package com.systemdesign.razorpay.merchant.api.MerchantWebhookApi;

import com.systemdesign.razorpay.common.dto.WebhookTarget;

import java.util.List;
import java.util.UUID;

public interface MerchantWebhookApi {
    List<WebhookTarget> getActiveConfigsForEvent(UUID merchantId, String eventType);
}
