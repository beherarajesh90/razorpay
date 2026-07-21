package com.systemdesign.razorpay.payment.processor.dto.response;

public sealed interface PaymentProcessorResponse
        permits PaymentProcessorResponse.Pending, PaymentProcessorResponse.Success, PaymentProcessorResponse.Failure {

    record Pending(String processorRef) implements PaymentProcessorResponse{}

    record Success(String processorRef, String bankReference) implements PaymentProcessorResponse{}

    record Failure(String errorCode, String errorDescription) implements PaymentProcessorResponse{}
}
