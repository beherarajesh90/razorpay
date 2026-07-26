package com.systemdesign.razorpay.payment.config;

import com.systemdesign.razorpay.common.enums.PaymentMethod;
import com.systemdesign.razorpay.payment.gateway.PaymentAdapter;
import com.systemdesign.razorpay.payment.gateway.adapter.CardPaymentAdapter;
import com.systemdesign.razorpay.payment.gateway.adapter.NetBankingPaymentAdapter;
import com.systemdesign.razorpay.payment.gateway.adapter.UpiPaymentAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class PaymentAdapterConfig {

    private final CardPaymentAdapter cardPaymentAdapter;
    private final NetBankingPaymentAdapter netBankingPaymentAdapter;
    private final UpiPaymentAdapter upiPaymentAdapter;

    @Bean
    public Map<PaymentMethod, PaymentAdapter> paymentAdapters() {
        return Map.of(
                PaymentMethod.CARD, cardPaymentAdapter,
                PaymentMethod.UPI, upiPaymentAdapter,
                PaymentMethod.NETBANKING, netBankingPaymentAdapter
        );
    }
}
