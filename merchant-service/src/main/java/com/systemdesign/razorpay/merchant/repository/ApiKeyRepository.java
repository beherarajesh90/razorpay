package com.systemdesign.razorpay.merchant.repository;

import com.systemdesign.razorpay.merchant.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByMerchant_Id(UUID merchantId);

    Optional<ApiKey> findByIdAndMerchant_Id(UUID keyId, UUID merchantId);

    Optional<ApiKey> findByKeyId(String keyId);
}
