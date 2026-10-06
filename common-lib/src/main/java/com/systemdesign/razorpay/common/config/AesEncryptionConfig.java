package com.systemdesign.razorpay.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Only services that hold encrypted data (merchant, vault) set vault.master-key.
 */
@Configuration
@ConditionalOnProperty(name = "vault.master-key")
public class AesEncryptionConfig {

    @Value("${vault.master-key}")
    private String masterKey;

    @Bean
    public BytesEncryptor masterKeyEncryptor() {
        byte[] masterKeyBytes = Base64.getDecoder().decode(masterKey);
        SecretKeySpec masterDecKey = new SecretKeySpec(masterKeyBytes, "AES/GCM/NoPadding");
        return new AesBytesEncryptor(masterDecKey, KeyGenerators.secureRandom(12),
                AesBytesEncryptor.CipherAlgorithm.GCM);
    }
}
