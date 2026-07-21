package com.systemdesign.razorpay.payment.repository;

import com.systemdesign.razorpay.payment.entity.OrderRecord;
import com.systemdesign.razorpay.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrder_Id(OrderRecord order);
}
