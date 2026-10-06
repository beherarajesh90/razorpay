package com.systemdesign.razorpay.merchant.dto.request;

import com.systemdesign.razorpay.common.enums.Environment;

public record CreateApiKeyRequest(
        Environment environment
) {
}
