package com.systemdesign.razorpay.payment.statemachine;

import com.systemdesign.razorpay.common.enums.PaymentActor;
import com.systemdesign.razorpay.common.enums.PaymentEvent;
import com.systemdesign.razorpay.common.enums.PaymentStatus;
import com.systemdesign.razorpay.payment.entity.Payment;
import com.systemdesign.razorpay.payment.entity.PaymentTransitionLog;
import com.systemdesign.razorpay.payment.repository.PaymentTransitionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentTransitionService {

    private final PaymentStateMachine paymentStateMachine;
    private final PaymentTransitionLogRepository paymentTransitionLogRepository;

    public PaymentStatus apply(Payment payment, PaymentEvent event){
        PaymentStatus previousPaymentStatus = payment.getStatus();
        PaymentStatus nextPaymentStatus = paymentStateMachine.transition(payment.getStatus(), event);
        payment.setStatus(nextPaymentStatus);

        PaymentTransitionLog log = PaymentTransitionLog.builder()
                .payment(payment)
                .fromStatus(previousPaymentStatus)
                .toStatus(nextPaymentStatus)
                .event(event)
                .actor(PaymentActor.SYSTEM)     // TODO: get merchant context to identify actor
                .occuredAt(LocalDateTime.now())
                .build();
        paymentTransitionLogRepository.save(log);

        return nextPaymentStatus;
    }
}
