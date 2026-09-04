package com.systemdesign.razorpay.payment.statemachine;

import com.systemdesign.razorpay.common.enums.RefundEvent;
import com.systemdesign.razorpay.common.enums.RefundStatus;
import com.systemdesign.razorpay.common.exception.InvalidStateTransitionException;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class RefundStateMachine {
    private record Transition(RefundStatus status, RefundEvent event) {}

    private static final Map<Transition, RefundStatus> TRANSITIONS = Map.of(
            new Transition(RefundStatus.PENDING, RefundEvent.PROCESS), RefundStatus.PROCESSING,
            new Transition(RefundStatus.PROCESSING, RefundEvent.PROCESS), RefundStatus.PROCESSING,
            new Transition(RefundStatus.PROCESSING, RefundEvent.SUCCESS), RefundStatus.PROCESSED,
            new Transition(RefundStatus.PROCESSING, RefundEvent.FAILURE), RefundStatus.FAILED
    );

    public RefundStatus transition(RefundStatus status, RefundEvent event) {
        RefundStatus next = TRANSITIONS.get(new Transition(status, event));
        if (next == null) throw new InvalidStateTransitionException(status.name(), event.name());
        return next;
    }
}
