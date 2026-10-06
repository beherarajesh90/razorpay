package com.systemdesign.razorpay.common.idempotency;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {

    String IN_PROGRESS = "IN_PROGRESS";

    boolean setIfAbsent(String key, Duration ttl);
    void store(String key, String value, Duration ttl);
    Optional<String> get(String key);
    void delete(String key);
}
