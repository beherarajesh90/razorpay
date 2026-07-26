package com.systemdesign.razorpay.payment.processor.strategy;

import com.systemdesign.razorpay.common.util.RandomizerUtil;
import com.systemdesign.razorpay.payment.processor.PaymentProcessor;
import com.systemdesign.razorpay.payment.processor.dto.request.PaymentProcessorRequest;
import com.systemdesign.razorpay.payment.processor.dto.response.PaymentProcessorResponse;
import org.springframework.stereotype.Component;

@Component
public class UpiPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {
        final String VPA_CODE_FAIL = "fail@okaxis";

        String bankCode = request.methodDetails() != null ? request.methodDetails().get("VPA").toString() : null;

        // simulation
        if(VPA_CODE_FAIL.equals(bankCode)){
            return new PaymentProcessorResponse.Failure("UPI_REJECTED",
                    "Bank rejected the transaction registration");
        }

        String processorRef = "UPI_PROCESSOR_" + RandomizerUtil.randomBase64(16);
        String bankRef = "BANK_REF_" + RandomizerUtil.randomBase64(16);
        return new PaymentProcessorResponse.Success(processorRef, bankRef);
    }
}
