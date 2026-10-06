package com.systemdesign.razorpay.common.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Crosses service boundaries (vault-service -> payment-service), so the subtype is serialized as "type".
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PaymentProcessorResponse.Pending.class, name = "PENDING"),
        @JsonSubTypes.Type(value = PaymentProcessorResponse.Success.class, name = "SUCCESS"),
        @JsonSubTypes.Type(value = PaymentProcessorResponse.Failure.class, name = "FAILURE")
})
public sealed interface PaymentProcessorResponse
        permits PaymentProcessorResponse.Pending, PaymentProcessorResponse.Success, PaymentProcessorResponse.Failure {

    record Pending(String processorRef) implements PaymentProcessorResponse{}

    record Success(String processorRef, String bankReference) implements PaymentProcessorResponse{}

    record Failure(String errorCode, String errorDescription) implements PaymentProcessorResponse{}
}
