package com.systemdesign.razorpay.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WebhookConfigRequest(

        @NotBlank(message = "Webhook URL is required")
        @Size(max = 500, message = "Webhook URL size should not exceed 500 characters")
        @Pattern(regexp = "^https?://.+")
        String targetUrl,

        // Comma-separated fine-grained event type names (e.g. "PAYMENT_STATUS_CHANGED,REFUND_CREATED").
        // Null/blank/"ALL" subscribes to every event type.
        @Size(max = 1000, message = "Event types size should not exceed 1000 characters")
        String eventTypes
) {
}
