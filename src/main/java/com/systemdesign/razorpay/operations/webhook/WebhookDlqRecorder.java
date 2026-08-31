package com.systemdesign.razorpay.operations.webhook;

import com.systemdesign.razorpay.operations.entity.DlqEvent;
import com.systemdesign.razorpay.operations.repository.DlqEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookDlqRecorder {

    private final DlqEventRepository dlqEventRepository;

    public void recordConsumerFailed(ConsumerRecord<String, Map<String, Object>> record, String errorMessage) {
        Map<String, Object> envelope = record.value();

        UUID merchantId = null;
        try {
            Map<String, Object> data = (Map<String, Object>) envelope.get("data");
            Object rawMerchantId = data!=null ? data.get("merchantId") : null;
            if(rawMerchantId!=null){
                merchantId = UUID.fromString(rawMerchantId.toString());
            }
        } catch (Exception ignored){}

        log.info("Recording the dlq because consumer failed event with merchantId: {}", merchantId);
        DlqEvent dlqEvent = DlqEvent.builder()
                .merchantId(merchantId)
                .webhookEvent(null)
                .finalError(errorMessage)
                .payload(envelope!=null ? envelope : Map.of())
                .build();
        dlqEventRepository.save(dlqEvent);
    }
}
