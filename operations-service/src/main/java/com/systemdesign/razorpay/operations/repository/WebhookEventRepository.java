package com.systemdesign.razorpay.operations.repository;

import com.systemdesign.razorpay.common.enums.WebhookEventStatus;
import com.systemdesign.razorpay.operations.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    List<WebhookEvent> findByStatusInAndNextRetryAtBefore(List<WebhookEventStatus> statuses, LocalDateTime nextRetryAt);
}
