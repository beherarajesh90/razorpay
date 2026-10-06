package com.systemdesign.razorpay.payment.repository;

import com.systemdesign.razorpay.payment.entity.Refund;
import com.systemdesign.razorpay.common.enums.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    Optional<Refund> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    Optional<Refund> findByIdAndMerchantId(UUID refundId, UUID merchantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Refund r where r.id = :refundId")
    Optional<Refund> findByIdForUpdate(@Param("refundId") UUID refundId);

    List<Refund> findTop100ByStatusInOrderByCreatedAtAsc(List<RefundStatus> statuses);

    @Query("select coalesce(sum(r.amount.amountUnits), 0) from Refund r " +
            "where r.payment.id = :paymentId and r.status in ('PENDING', 'PROCESSING', 'PROCESSED')")
    Long sumReservedAmount(@Param("paymentId") UUID paymentId);
}
