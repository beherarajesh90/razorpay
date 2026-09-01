package com.systemdesign.razorpay.operations.repository;

import com.systemdesign.razorpay.operations.entity.SettlementPayment;
import com.systemdesign.razorpay.operations.entity.SettlementPaymentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementPaymentRepository extends JpaRepository<SettlementPayment, SettlementPaymentId> {
}