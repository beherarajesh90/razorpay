package com.systemdesign.razorpay.vault.processor;

import com.systemdesign.razorpay.common.dto.PaymentProcessorRequest;
import com.systemdesign.razorpay.common.dto.PaymentProcessorResponse;
import com.systemdesign.razorpay.common.util.RandomizerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Card acquiring simulator. Lives in vault-service because only vault sees the PAN.
 */
@Component
@Slf4j
public class CardPaymentProcessor {

    public static final String PAN_CARD_DECLINED = "4000000000000002";
    public static final String PAN_CARD_EXPIRED = "4000000000000069";

    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {

        String pan = request.pan();

        if (PAN_CARD_DECLINED.equals(pan)) {
            log.warn("Card declined");
            return new PaymentProcessorResponse.Failure("CARD_DECLINED", "Card declined by bank");
        }

        if (PAN_CARD_EXPIRED.equals(pan)) {
            log.warn("Pan card has expired");
            return new PaymentProcessorResponse.Failure("CARD_EXPIRED", "Card has expired");
        }

        String processorRef = "CARD_PROCESSOR_" + RandomizerUtil.randomBase64(16);

        return new PaymentProcessorResponse.Pending(processorRef);
    }
}
