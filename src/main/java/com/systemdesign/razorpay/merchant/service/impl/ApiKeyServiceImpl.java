package com.systemdesign.razorpay.merchant.service.impl;

import com.systemdesign.razorpay.common.exception.ResourceNotFoundException;
import com.systemdesign.razorpay.common.util.RandomizerUtil;
import com.systemdesign.razorpay.merchant.dto.request.CreateApiKeyRequest;
import com.systemdesign.razorpay.merchant.dto.response.ApiKeyCreateResponse;
import com.systemdesign.razorpay.merchant.dto.response.ApiKeyResponse;
import com.systemdesign.razorpay.merchant.entity.ApiKey;
import com.systemdesign.razorpay.merchant.entity.Merchant;
import com.systemdesign.razorpay.merchant.mapper.ApiKeyMapper;
import com.systemdesign.razorpay.merchant.repository.ApiKeyRepository;
import com.systemdesign.razorpay.merchant.repository.MerchantRepository;
import com.systemdesign.razorpay.merchant.service.ApiKeyService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApiKeyServiceImpl implements ApiKeyService {

    private final MerchantRepository merchantRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;

    @Override
    @Transactional
    public ApiKeyCreateResponse create(UUID merchantId, CreateApiKeyRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(
                () -> new ResourceNotFoundException("merchant", merchantId)
        );

        String keyId = "rzp_"+request.environment().name().toLowerCase()+"_"+ RandomizerUtil.randomBase64(24);
        String rawSecret = RandomizerUtil.randomBase64(40);

        ApiKey apiKey = ApiKey.builder()
                .merchant(merchant)
                .keyId(keyId)
                .keySecretHash(rawSecret)                    // TODO: encode with BcryptPasswordEncoder
                .environment(request.environment())
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        return new ApiKeyCreateResponse(apiKey.getId(), keyId, rawSecret, request.environment());
    }

    @Override
    public List<ApiKeyResponse> listByMerchant(UUID merchantId) {
        return apiKeyMapper.toResponseList(apiKeyRepository.findByMerchant_Id(merchantId));
    }

    @Override
    @Transactional
    public void revoke(UUID merchantId, UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndMerchant_Id(keyId, merchantId).orElseThrow(
                () -> new ResourceNotFoundException("ApiKey", keyId)
        );
        apiKey.setEnabled(false);
    }

    @Override
    @Transactional
    public @Nullable ApiKeyCreateResponse rotate(UUID merchantId, UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndMerchant_Id(merchantId, keyId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId));

        if(!apiKey.isEnabled()) throw new RuntimeException("cannot rotate a disabled key");

        String newRawSecret = RandomizerUtil.randomBase64(40);
        apiKey.setPreviousKeySecretHash(apiKey.getKeySecretHash());
        apiKey.setKeySecretHash(newRawSecret);                      // TODO: encode with BcryptPasswordEncoder
        apiKey.setRotatedAt(LocalDateTime.now());
        apiKey.setGracePeriodExpiresAt(LocalDateTime.now().plusHours(24));
        apiKey = apiKeyRepository.save(apiKey);

        return new ApiKeyCreateResponse(apiKey.getId(), apiKey.getKeyId(), apiKey.getKeySecretHash(), apiKey.getEnvironment());
    }
}
