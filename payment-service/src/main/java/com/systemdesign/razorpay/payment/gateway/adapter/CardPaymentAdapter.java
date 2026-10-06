package com.systemdesign.razorpay.payment.gateway.adapter;

import com.systemdesign.razorpay.payment.gateway.PaymentAdapter;
import com.systemdesign.razorpay.common.entity.Money;
import com.systemdesign.razorpay.payment.gateway.dto.request.PaymentRequest;
import com.systemdesign.razorpay.payment.gateway.dto.respose.PaymentResult;
import com.systemdesign.razorpay.common.dto.PaymentProcessorResponse;
import com.systemdesign.razorpay.common.dto.VaultChargeRequest;
import com.systemdesign.razorpay.payment.client.VaultServiceClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardPaymentAdapter implements PaymentAdapter {

    private final VaultServiceClient vaultServiceClient;

    @Override
    public PaymentResult initiate(PaymentRequest request) {
        String token = (String) request.methodDetails().get("token");

        PaymentProcessorResponse paymentProcessorResponse;
        try {
            paymentProcessorResponse = vaultServiceClient.charge(
                    new VaultChargeRequest(request.paymentId(), token, request.amount(), request.methodDetails()));
        } catch (FeignException e) {
            log.error("vault-service call failed for paymentId={}", request.paymentId(), e);
            return new PaymentResult.Failure("VAULT_UNAVAILABLE", "Card vault could not process the charge");
        }

        return switch (paymentProcessorResponse){
            case PaymentProcessorResponse.Success success -> new PaymentResult.Success(success.bankReference());
            case PaymentProcessorResponse.Failure failure -> new PaymentResult.Failure(failure.errorCode(), failure.errorDescription());
            case PaymentProcessorResponse.Pending pending -> new PaymentResult.Pending(pending.processorRef());
        };
    }

    @Override
    public PaymentResult capture(UUID paymentId) {
        return new PaymentResult.Success("CARD_REF");
    }

    @Override
    public PaymentResult refund(UUID paymentId, Money amount) {
        return new PaymentResult.Success("CARD_REFUND_REF");
    }
}
