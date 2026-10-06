package com.systemdesign.razorpay.operations;

import com.sun.net.httpserver.HttpServer;
import com.systemdesign.razorpay.common.dto.PaymentSettlementView;
import com.systemdesign.razorpay.common.dto.SettlementBankDetails;
import com.systemdesign.razorpay.common.dto.WebhookTarget;
import com.systemdesign.razorpay.common.entity.Money;
import com.systemdesign.razorpay.operations.client.MerchantServiceClient;
import com.systemdesign.razorpay.operations.client.PaymentServiceClient;
import com.systemdesign.razorpay.operations.settlement.SettlementEngine;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Settlement batch, settlement re-run exclusion, and Kafka event -> webhook delivery.
 * Merchant and payment are mocked (other services). Postgres, Redis and Kafka are real containers.
 */
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@Testcontainers
class OperationsFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Container
    @ServiceConnection
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.5.0");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @MockitoBean
    MerchantServiceClient merchantServiceClient;

    @MockitoBean
    PaymentServiceClient paymentServiceClient;

    @Autowired
    SettlementEngine settlementEngine;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void settlementRunLinksPaymentsAndDoesNotSettleThemTwice() {
        UUID merchantId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(merchantServiceClient.listActiveMerchantIds()).thenReturn(List.of(merchantId));
        when(merchantServiceClient.getSettlementBankDetails(merchantId))
                .thenReturn(new SettlementBankDetails("123456789012", "HDFC0001234", "IT Merchant"));
        when(paymentServiceClient.findCapturedPayments(merchantId))
                .thenReturn(List.of(new PaymentSettlementView(paymentId, merchantId, Money.of(1000, "INR"))));

        settlementEngine.run();

        Integer settlements = jdbc.queryForObject(
                "select count(*) from settlement where merchant_id = ?", Integer.class, merchantId);
        assertThat(settlements).isEqualTo(1);

        String status = jdbc.queryForObject(
                "select status from settlement where merchant_id = ?", String.class, merchantId);
        assertThat(status).isIn("TRANSFER_PENDING", "PROCESSED");

        Integer links = jdbc.queryForObject(
                "select count(*) from settlement_payment where payment_id = ?", Integer.class, paymentId);
        assertThat(links).isEqualTo(1);

        // Same payment still reported as captured: must be excluded, not settled again.
        settlementEngine.run();

        Integer settlementsAfterRerun = jdbc.queryForObject(
                "select count(*) from settlement where merchant_id = ?", Integer.class, merchantId);
        assertThat(settlementsAfterRerun).isEqualTo(1);
    }

    @Test
    void paymentEventIsDeliveredToMerchantWebhook() throws IOException {
        UUID merchantId = UUID.randomUUID();
        ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();

        HttpServer receiver = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        receiver.createContext("/hook", exchange -> {
            received.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        receiver.start();
        try {
            String url = "http://localhost:" + receiver.getAddress().getPort() + "/hook";
            when(merchantServiceClient.getActiveConfigsForEvent(any(UUID.class), anyString()))
                    .thenReturn(List.of(new WebhookTarget(UUID.randomUUID(), url, "whsec_test")));

            kafkaTemplate.send("payments.events", merchantId.toString(), Map.of(
                    "eventType", "PAYMENT_CREATED",
                    "data", Map.of("merchantId", merchantId.toString(), "paymentId", UUID.randomUUID().toString())));

            Awaitility.await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
                assertThat(received).isNotEmpty();
                assertThat(received.peek()).contains("PAYMENT_CREATED");
            });

            String delivered = jdbc.queryForObject(
                    "select status from webhook_event where event_type = 'PAYMENT_CREATED' order by created_at desc limit 1",
                    String.class);
            assertThat(delivered).isEqualTo("DELIVERED");
        } finally {
            receiver.stop(0);
        }
    }
}
