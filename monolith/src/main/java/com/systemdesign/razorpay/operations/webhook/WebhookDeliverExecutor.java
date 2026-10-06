package com.systemdesign.razorpay.operations.webhook;

import com.systemdesign.razorpay.common.enums.WebhookEventStatus;
import com.systemdesign.razorpay.operations.entity.WebhookEvent;
import com.systemdesign.razorpay.operations.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDeliverExecutor {

    private static final Integer MAX_ATTEMPTS = 7;
    private static final List<Duration> BACK_OFF = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5),
            Duration.ofMinutes(30), Duration.ofHours(2), Duration.ofHours(8), Duration.ofMinutes(24));

    private final WebhookEventRepository webhookEventRepository;
    private final RestClient restClient;
    private final WebhookDlqRecorder webhookDlqRecorder;
    private final WebhookRetryQueue webhookRetryQueue;

    @Value("${webhook.delivery.signature-header:X-Razorpay-Header}")
    private String signatureHeader;

    @Transactional
    public void deliver(UUID webhookEventId){
        Optional<WebhookEvent> event = webhookEventRepository.findById(webhookEventId);

        if(!event.isPresent()){
            log.warn("No webhook event found for this id: {}", webhookEventId);
            return;
        }

        WebhookEvent webhookEvent = event.get();

        if(webhookEvent.getStatus() == WebhookEventStatus.DELIVERED || webhookEvent.getStatus() == WebhookEventStatus.DEAD){
            log.warn("Cannot deliver the webhook event with id:{}, status:{}",webhookEventId, webhookEvent.getStatus());
            return;
        }

        webhookEvent.setAttempts(webhookEvent.getAttempts() + 1);
        webhookEvent.setLastAttemptAt(LocalDateTime.now());

        try {
            var response = restClient.post()
                    .uri(webhookEvent.getTargetUrl())
                    .header(signatureHeader, webhookEvent.getSignature())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("event", webhookEvent.getEventType(),
                            "payload", webhookEvent.getPayload()))
                    .retrieve()
                    .toBodilessEntity();
            int statusCode = response.getStatusCode().value();
            webhookEvent.setLastResponseCode(statusCode);

            if(response.getStatusCode().is2xxSuccessful()){
                webhookEvent.setStatus(WebhookEventStatus.DELIVERED);
                webhookEvent.setDeliveredAt(LocalDateTime.now());
                log.info("Successfully called the merchant for webhook event: {}", webhookEventId);
                return;
            }

            handleAttemptFailed(webhookEvent, "HTTP"+statusCode);
        } catch (RestClientException e){
            webhookEvent.setLastResponseBody(e.getMessage());
            handleAttemptFailed(webhookEvent, e.getMessage());
            log.error("Error while delivering webhook event: {}, error: {}", webhookEventId, e.getMessage());
        }
    }

    private void handleAttemptFailed(WebhookEvent webhookEvent, String error) {
        webhookEvent.setLastResponseBody(error);

        if(webhookEvent.getAttempts() >= MAX_ATTEMPTS){
            webhookEvent.setStatus(WebhookEventStatus.DEAD);
            webhookDlqRecorder.recordAfterAttemptsExhausted(webhookEvent, error);
            return;
        }

        Duration backoff = BACK_OFF.get(webhookEvent.getAttempts()-1);
        LocalDateTime nextRetryAt = LocalDateTime.now().plus(backoff);
        webhookEvent.setStatus(WebhookEventStatus.FAILED);
        webhookEvent.setNextRetryAt(nextRetryAt);
        webhookEventRepository.save(webhookEvent);

        webhookRetryQueue.enqueue(webhookEvent.getId(), nextRetryAt);

        log.error("Handling attempt failed for webhook event {} with attempts: {}, Next Retry at: {}",
                webhookEvent.getId(), webhookEvent.getAttempts(), nextRetryAt);
    }
}
