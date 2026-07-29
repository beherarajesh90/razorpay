package com.systemdesign.razorpay.payment.simulator;

import com.systemdesign.razorpay.common.enums.ChaosMode;
import com.systemdesign.razorpay.common.enums.PaymentStatus;
import com.systemdesign.razorpay.common.util.RandomizerUtil;
import com.systemdesign.razorpay.payment.entity.Payment;
import com.systemdesign.razorpay.payment.repository.PaymentRepository;
import com.systemdesign.razorpay.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BankCallbackSimulator {

    private final SimulatorConfig simulatorConfig;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @Scheduled(fixedDelayString = "${payment.simulator.poll-interval-ms:5000}")
    public void processCallbacks(){
        LocalDateTime globalWindow = LocalDateTime.now().minusSeconds(1);

        List<Payment> candidates = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.AUTHORIZING, globalWindow);

        log.info("Simulating payments for {} payments", candidates.size());

        if(CollectionUtils.isEmpty(candidates)){
            return;
        }

        for (Payment payment: candidates){
            simulateCallback(payment);
        }
    }

    private void simulateCallback(Payment payment) {
        SimulatorConfig.MethodSimulatorConfig methodConfig = simulatorConfig.getMethods().get(payment.getMethod());

        LocalDateTime dueAt = dueAt(payment, methodConfig);
        if(LocalDateTime.now().isBefore(dueAt)){
            return;
        }

        ChaosMode chaosMode = simulatorConfig.getChaosMode();

        switch (chaosMode){
            case SUCCESS -> resolve(payment, true);
            case FAILURE -> resolve(payment, false);
            case TIMEOUT -> {
                log.debug("BankCallback simulator: Payment Timed Out");
            }
            case NORMAL, SLOW -> resolve(payment, shouldApprove(payment, methodConfig));
        }
    }

    private boolean shouldApprove(Payment payment, SimulatorConfig.MethodSimulatorConfig methodConfig) {
        int bucket = Math.abs(payment.getId().hashCode()) % 100;
        return bucket < methodConfig.getSuccessRate();
    }

    private void resolve(Payment payment, boolean approve) {
        if(approve){
            String bankRef = "SIM_BANK_REF"+ RandomizerUtil.randomBase64(8);
            paymentService.resolveAuthorization(payment.getId(), true, bankRef, null, null);
        } else{
            paymentService.resolveAuthorization(payment.getId(), false, null, "SIMULATOR_BANK_ERROR_CODE", "Simulator bank declined");
        }
    }

    private LocalDateTime dueAt(Payment payment, SimulatorConfig.MethodSimulatorConfig methodConfig) {
        int range = methodConfig.getMaxDelaySeconds() - methodConfig.getMinDelaySeconds();
        int delay = methodConfig.getMinDelaySeconds() + Math.abs(payment.getId().hashCode()) % (range + 1);

        if(ChaosMode.SLOW == simulatorConfig.getChaosMode()){
            delay *= 2;
        }

        return payment.getCreatedAt().plusSeconds(delay);
    }

}
