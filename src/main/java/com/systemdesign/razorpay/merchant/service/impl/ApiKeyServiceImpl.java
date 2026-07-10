package com.systemdesign.razorpay.merchant.service.impl;

import com.systemdesign.razorpay.common.exception.ResourceNotFoundException;
import com.systemdesign.razorpay.merchant.dto.request.CreateApiKeyRequest;
import com.systemdesign.razorpay.merchant.dto.response.ApiKeyCreateResponse;
import com.systemdesign.razorpay.merchant.entity.ApiKey;
import com.systemdesign.razorpay.merchant.entity.Merchant;
import com.systemdesign.razorpay.merchant.repository.ApiKeyRepository;
import com.systemdesign.razorpay.merchant.repository.MerchantRepository;
import com.systemdesign.razorpay.merchant.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private final MerchantRepository merchantRepository;
    private final ApiKeyRepository apiKeyRepository;

    @Override
    public ApiKeyCreateResponse create(UUID merchantId, CreateApiKeyRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId).orElseThrow(
                () -> new ResourceNotFoundException("merchant", merchantId)
        );

        String keyId = "rzp_"+request.environment().name().toUpperCase()+"random_secret";
        String rawSecret = "random_secret_hash";    // TODO: replace with cryptographic random hex

        ApiKey apiKey = ApiKey.builder()
                .merchant(merchant)
                .keyId(keyId)
                .keySecretHash(rawSecret)                    // TODO: encode with BcryptPasswordEncoder
                .environment(request.environment())
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        return new ApiKeyCreateResponse(apiKey.getId(), keyId, rawSecret, request.environment());
    }
}
