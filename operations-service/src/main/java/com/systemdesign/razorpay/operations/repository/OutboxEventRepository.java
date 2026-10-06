package com.systemdesign.razorpay.operations.repository;

import com.systemdesign.razorpay.common.enums.OutboxStatus;
import com.systemdesign.razorpay.operations.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
