package com.systemdesign.razorpay.operations.repository;

import com.systemdesign.razorpay.common.enums.SettlementStatus;
import com.systemdesign.razorpay.operations.entity.SettlementPayment;
import com.systemdesign.razorpay.operations.entity.SettlementPaymentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SettlementPaymentRepository extends JpaRepository<SettlementPayment, SettlementPaymentId> {

    /**
     * Payments already included in a settlement for this merchant, ignoring failed settlements
     * so their payments can be picked up again.
     */
    @Query("select sp.id.paymentId from SettlementPayment sp " +
            "where sp.settlement.merchantId = :merchantId and sp.settlement.status <> :excludedStatus")
    List<UUID> findSettledPaymentIdsByMerchantId(@Param("merchantId") UUID merchantId,
                                                 @Param("excludedStatus") SettlementStatus excludedStatus);
}
