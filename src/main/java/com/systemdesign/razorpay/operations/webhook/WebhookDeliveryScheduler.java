package com.systemdesign.razorpay.operations.webhook;

import com.systemdesign.razorpay.common.enums.WebhookEventStatus;
import com.systemdesign.razorpay.operations.entity.WebhookEvent;
import com.systemdesign.razorpay.operations.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookDeliveryScheduler {

    private final WebhookRetryQueue webhookRetryQueue;
    private final WebhookEventRepository webhookEventRepository;

    @Value("${app.webhook.delivery.poll-batch-size:100}")
    private int batchSize = 100;

    @Scheduled(fixedDelay = 1000)
    public void pollAndDeliver(){
        Set<UUID> dueEvents = webhookRetryQueue.pollDue(batchSize);

        if(dueEvents.isEmpty()){
            log.debug("No due webhook events found for delivery");
            return;
        }

        for (UUID webhookEventId : dueEvents){
            // executor.deliver(webhookEventId);
        }
    }

    // in case if any pending events are left in the queue, we can schedule a retry for them
    @Scheduled(fixedDelay = 10000)
    public void reconcileFromDatabase(){
        List<WebhookEvent> dueEvents = webhookEventRepository.findByStatusAndNextRetryAtBefore(WebhookEventStatus.PENDING, LocalDateTime.now());

        for(WebhookEvent event: dueEvents){
            webhookRetryQueue.enqueueIfAbsent(event.getId(), event.getNextRetryAt());
        }
    }
}

