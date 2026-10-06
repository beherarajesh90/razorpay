package com.systemdesign.razorpay.payment.gateway.adapter;

import com.systemdesign.razorpay.common.enums.PaymentMethod;
import com.systemdesign.razorpay.common.entity.Money;
import com.systemdesign.razorpay.payment.gateway.PaymentAdapter;
import com.systemdesign.razorpay.payment.gateway.dto.request.PaymentRequest;
import com.systemdesign.razorpay.payment.gateway.dto.respose.PaymentResult;
import com.systemdesign.razorpay.payment.processor.PaymentProcessorRouter;
import com.systemdesign.razorpay.common.dto.PaymentProcessorRequest;
import com.systemdesign.razorpay.common.dto.PaymentProcessorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class NetBankingPaymentAdapter implements PaymentAdapter {

    private final PaymentProcessorRouter paymentProcessorRouter;

    @Override
    public PaymentResult initiate(PaymentRequest request) {
        log.info("Initiating payment with NetBanking, paymentId: {}", request.paymentId());

        try {
            PaymentProcessorRequest paymentProcessorRequest = PaymentProcessorRequest.nonCard(
                    request.paymentId(),
                    PaymentMethod.NET_BANKING,
                    request.amount(),
                    request.methodDetails()
            );

            PaymentProcessorResponse paymentProcessorResponse = paymentProcessorRouter.charge(paymentProcessorRequest);

            return switch (paymentProcessorResponse){
                case PaymentProcessorResponse.Failure failure -> new PaymentResult.Failure(failure.errorCode(), failure.errorDescription());
                case PaymentProcessorResponse.Pending pending -> new PaymentResult.Pending(pending.processorRef());
                case PaymentProcessorResponse.Success success -> new PaymentResult.Success(success.bankReference());
            };
        } catch (Exception ex){
            log.warn("NetBanking failed, paymentId: {}", request.paymentId());
            return new PaymentResult.Failure("NBK_FAILED", ex.getMessage());
        }
    }

    @Override
    public PaymentResult capture(UUID paymentId) {
        return new PaymentResult.Success("NBK_REF");
    }

    @Override
    public PaymentResult refund(UUID paymentId, Money amount) {
        return new PaymentResult.Success("NBK_REFUND_REF");
    }
}
