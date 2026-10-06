package com.systemdesign.razorpay.payment;

import com.jayway.jsonpath.JsonPath;
import com.systemdesign.razorpay.common.dto.PaymentProcessorResponse;
import com.systemdesign.razorpay.common.dto.VaultChargeRequest;
import com.systemdesign.razorpay.payment.client.CustomerServiceClient;
import com.systemdesign.razorpay.payment.client.VaultServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Order -> payment -> card charge. Customer and vault are mocked: they are other services.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class PaymentFlowIntegrationTest {

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

    @MockitoBean
    VaultServiceClient vaultServiceClient;

    @MockitoBean
    CustomerServiceClient customerServiceClient;

    @Autowired
    MockMvc mvc;

    @Test
    void orderThenCardPaymentCallsVaultAndStoresPayment() throws Exception {
        UUID merchantId = UUID.randomUUID();
        when(vaultServiceClient.charge(any(VaultChargeRequest.class)))
                .thenReturn(new PaymentProcessorResponse.Pending("CARD_PROCESSOR_IT"));

        String order = mvc.perform(post("/v1/orders")
                        .header("X-Merchant-Id", merchantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":{\"amountUnits\":1000,\"currency\":\"INR\"},\"receipt\":\"it-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantId").value(merchantId.toString()))
                .andReturn().getResponse().getContentAsString();
        String orderId = JsonPath.read(order, "$.id");

        mvc.perform(post("/v1/payments")
                        .header("X-Merchant-Id", merchantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"" + orderId + "\",\"method\":\"CARD\",\"methodDetails\":{\"token\":\"tok_it\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.method").value("CARD"));

        verify(vaultServiceClient).charge(any(VaultChargeRequest.class));
    }
}
