package com.systemdesign.razorpay.operations.settlement;

import com.systemdesign.razorpay.common.entity.Money;
import com.systemdesign.razorpay.operations.settlement.dto.BankTransferResult;

import java.util.UUID;

public interface BankTransferProcessor {

    BankTransferResult initiate(UUID settlementId, UUID merchantId, Money amount,
                                String bankAccount, String ifsc);
}
