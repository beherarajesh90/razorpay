package com.systemdesign.razorpay.merchant.api;

import com.systemdesign.razorpay.common.dto.SettlementBankDetails;
import com.systemdesign.razorpay.common.dto.WebhookTarget;

import java.util.List;
import java.util.UUID;

public interface MerchantLookupService {
    List<WebhookTarget> getActiveConfigsForEvent(UUID merchantId, String eventType);

    List<UUID> listActiveMerchantIds();

    SettlementBankDetails getSettlementBankDetails(UUID merchantId);
}
