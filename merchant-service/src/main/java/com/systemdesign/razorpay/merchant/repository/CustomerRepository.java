package com.systemdesign.razorpay.merchant.repository;

import com.systemdesign.razorpay.merchant.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByMerchant_IdAndEmail(UUID merchantId, String email);
}
