package com.systemdesign.razorpay.merchant;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "app.rate-limit.method=token-bucket",
        "app.rate-limit.use-case.api-key.max-requests=1000",
        "app.rate-limit.use-case.api-key.window-seconds=10"
})
@AutoConfigureMockMvc
@Testcontainers
class MerchantFlowIntegrationTest {

    /** Fresh random key per test run. Never a literal, so no secret is committed. */
    static final String TEST_VAULT_KEY = randomKey(32);
    static final String TEST_JWT_KEY = randomKey(64);

    static String randomKey(int bytes) {
        byte[] b = new byte[bytes];
        new java.security.SecureRandom().nextBytes(b);
        return java.util.Base64.getEncoder().encodeToString(b);
    }

    @DynamicPropertySource
    static void keyProperties(DynamicPropertyRegistry registry) {
        registry.add("vault.master-key", () -> TEST_VAULT_KEY);
        registry.add("jwt.secret-key", () -> TEST_JWT_KEY);
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
    void signupLoginApiKeyAndVerify() throws Exception {
        String email = "it-" + UUID.randomUUID() + "@example.com";
        String password = "password123";

        String signup = mvc.perform(post("/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"IT\",\"email\":\"" + email + "\",\"password\":\"" + password
                                + "\",\"businessName\":\"IT Co\",\"businessType\":\"PROPRIETORSHIP\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String merchantId = JsonPath.read(signup, "$.id");

        String login = mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(login, "$.accessToken");

        mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));

        String key = mvc.perform(post("/v1/merchants/" + merchantId + "/api-keys")
                        .header("X-Merchant-Id", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"environment\":\"TEST\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String keyId = JsonPath.read(key, "$.keyId");
        String keySecret = JsonPath.read(key, "$.keySecret");

        String basic = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

        mvc.perform(post("/internal/v1/auth/verify").header("Authorization", "Basic " + basic))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andExpect(jsonPath("$.keyId").value(keyId));

        String wrongBasic = Base64.getEncoder().encodeToString((keyId + ":not-the-secret").getBytes(StandardCharsets.UTF_8));
        mvc.perform(post("/internal/v1/auth/verify").header("Authorization", "Basic " + wrongBasic))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/internal/v1/auth/verify").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andExpect(jsonPath("$.subject").value(email));
    }
}
