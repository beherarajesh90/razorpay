package com.systemdesign.razorpay.payment.config;

import com.systemdesign.razorpay.common.enums.PaymentMethod;
import com.systemdesign.razorpay.payment.processor.PaymentProcessor;
import com.systemdesign.razorpay.payment.processor.strategy.NetBankingPaymentProcessor;
import com.systemdesign.razorpay.payment.processor.strategy.UpiPaymentProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PaymentProcessorConfig {

    private final UpiPaymentProcessor upiPaymentProcessor;
    private final NetBankingPaymentProcessor netBankingPaymentProcessor;


    @Bean
    public Map<PaymentMethod, PaymentProcessor> paymentProcessors(){
        return Map.of(
                PaymentMethod.UPI, upiPaymentProcessor,
                PaymentMethod.NET_BANKING, netBankingPaymentProcessor
        );
    }
}
