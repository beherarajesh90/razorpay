package com.systemdesign.razorpay.merchant.dto.response;

import com.systemdesign.razorpay.common.enums.BusinessType;
import com.systemdesign.razorpay.common.enums.MerchantStatus;

import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String name,
        String email,
        String businessName,
        BusinessType businessType,
        MerchantStatus merchantStatus
) {
}
