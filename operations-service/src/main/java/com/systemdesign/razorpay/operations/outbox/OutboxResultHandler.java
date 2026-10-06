package com.systemdesign.razorpay.operations.outbox;

import com.systemdesign.razorpay.common.enums.OutboxStatus;
import com.systemdesign.razorpay.operations.entity.OutboxEvent;
import com.systemdesign.razorpay.operations.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxResultHandler {

    private static final int MAX_ATTEMPTS = 3;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void handleEventPublished(OutboxEvent event) {
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(LocalDateTime.now());
        outboxEventRepository.save(event);
    }

    @Transactional
    public void handleEventFailed(OutboxEvent event, String errorMessage) {
        event.setAttempts(event.getAttempts() + 1);
        event.setLastError(errorMessage.length() < 1000 ? errorMessage : errorMessage.substring(0, 1000));
        if(event.getAttempts() >= MAX_ATTEMPTS) {
            event.setStatus(OutboxStatus.FAILED);
        }
        outboxEventRepository.save(event);
    }
}
