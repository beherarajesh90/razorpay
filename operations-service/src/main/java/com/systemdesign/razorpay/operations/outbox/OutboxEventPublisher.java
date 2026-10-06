package com.systemdesign.razorpay.operations.outbox;


import com.systemdesign.razorpay.common.enums.EventAggregateType;
import com.systemdesign.razorpay.operations.entity.OutboxEvent;
import com.systemdesign.razorpay.operations.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;

    public void publish(EventAggregateType aggregateType, UUID aggregateId, String eventType,
                        Map<String, Object> payload) {
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .eventType(eventType)
                .payload(payload)
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}
