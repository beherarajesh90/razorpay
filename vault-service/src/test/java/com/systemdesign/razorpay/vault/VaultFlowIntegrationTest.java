package com.systemdesign.razorpay.vault;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
})
@AutoConfigureMockMvc
@Testcontainers
class VaultFlowIntegrationTest {

    /** Fresh random key per test run. Never a literal, so no secret is committed. */
    static final String TEST_VAULT_KEY = randomKey(32);

    static String randomKey(int bytes) {
        byte[] b = new byte[bytes];
        new java.security.SecureRandom().nextBytes(b);
        return java.util.Base64.getEncoder().encodeToString(b);
    }

    @DynamicPropertySource
    static void keyProperties(DynamicPropertyRegistry registry) {
        registry.add("vault.master-key", () -> TEST_VAULT_KEY);
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    MockMvc mvc;

    @Test
    void tokenizeThenInternalChargeReturnsPending() throws Exception {
        String merchantId = UUID.randomUUID().toString();
        String customerId = UUID.randomUUID().toString();

        String tokenize = mvc.perform(post("/v1/vault/tokenize")
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pan\":\"4111111111111111\",\"cvv\":\"123\",\"expiryMonth\":12,"
                                + "\"expiryYear\":2030,\"customerId\":\"" + customerId + "\",\"cardHolderName\":\"IT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lastFour").value("1111"))
                .andExpect(jsonPath("$.brand").value("VISA"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(tokenize, "$.token");

        mvc.perform(post("/internal/v1/vault/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"" + UUID.randomUUID() + "\",\"token\":\"" + token + "\","
                                + "\"amount\":{\"amountUnits\":1000,\"currency\":\"INR\"},\"methodDetails\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PENDING"))
                .andExpect(jsonPath("$.processorRef").exists());

        mvc.perform(post("/internal/v1/vault/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"" + UUID.randomUUID() + "\",\"token\":\"tok_unknown\","
                                + "\"amount\":{\"amountUnits\":1000,\"currency\":\"INR\"},\"methodDetails\":{}}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CARDTOKEN_NOT_FOUND"));
    }
}
