package com.systemdesign.razorpay.operations.entity;

import com.systemdesign.razorpay.common.entity.BaseEntity;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class SettlementPaymentId {
    private UUID settlementId;
    private UUID paymentId;
}
