package com.systemdesign.razorpay.payment.processor.strategy;

import com.systemdesign.razorpay.common.util.RandomizerUtil;
import com.systemdesign.razorpay.payment.processor.PaymentProcessor;
import com.systemdesign.razorpay.common.dto.PaymentProcessorRequest;
import com.systemdesign.razorpay.common.dto.PaymentProcessorResponse;
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

        return new PaymentProcessorResponse.Pending(processorRef);
    }
}
