package com.systemdesign.razorpay.operations;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class SettlementPaymentId {
    private UUID settlementId;
    private UUID paymentId;
}
