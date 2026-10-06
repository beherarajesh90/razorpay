package com.systemdesign.razorpay.operations.settlement;

import com.systemdesign.razorpay.common.dto.SettlementBankDetails;
import com.systemdesign.razorpay.common.entity.Money;
import com.systemdesign.razorpay.common.enums.EventAggregateType;
import com.systemdesign.razorpay.common.enums.SettlementStatus;
import com.systemdesign.razorpay.common.exception.ResourceNotFoundException;
import com.systemdesign.razorpay.operations.client.MerchantServiceClient;
import com.systemdesign.razorpay.operations.entity.Settlement;
import com.systemdesign.razorpay.operations.entity.SettlementPayment;
import com.systemdesign.razorpay.operations.entity.SettlementPaymentId;
import com.systemdesign.razorpay.operations.repository.SettlementPaymentRepository;
import com.systemdesign.razorpay.operations.repository.SettlementRepository;
import com.systemdesign.razorpay.operations.settlement.dto.BankTransferResult;
import com.systemdesign.razorpay.common.dto.PaymentSettlementView;
import com.systemdesign.razorpay.operations.client.PaymentServiceClient;
import com.systemdesign.razorpay.operations.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementTransactionExecutor {

    private static final double FEE_RATE = 0.02;
    private static final double GST_RATE = 0.18;

    private final PaymentServiceClient paymentServiceClient;
    private final SettlementRepository settlementRepository;
    private final SettlementPaymentRepository settlementPaymentRepository;
    private final MerchantServiceClient merchantServiceClient;
    private final BankTransferProcessor bankTransferProcessor;
    // Todo: publisher inside it's own db
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public void processForMerchant(UUID merchantId, LocalDate settlementDate) {
        Set<UUID> alreadySettled = Set.copyOf(settlementPaymentRepository.findSettledPaymentIdsByMerchantId(merchantId, SettlementStatus.FAILED));
        List<PaymentSettlementView> unsettledPayments = paymentServiceClient.findCapturedPayments(merchantId).stream()
                .filter(p -> !alreadySettled.contains(p.paymentId()))
                .toList();
        if (unsettledPayments.isEmpty()) return;

        log.info("Processing {} unsettled payments for merchantId: {} on {} date",
                unsettledPayments.size(), merchantId, settlementDate);

        Money gross = unsettledPayments.stream()
                .map(PaymentSettlementView::amount)
                .reduce(Money::add)
                .orElseThrow();

        int fee = Math.toIntExact(Math.round(gross.getAmountUnits() * FEE_RATE));
        int gst = Math.toIntExact(Math.round(fee * GST_RATE));
        Money feeAmount = Money.of(fee, gross.getCurrency());
        Money gstAmount = Money.of(gst, gross.getCurrency());
        Money netAmount = gross.subtract(feeAmount).subtract(gstAmount);

        Settlement settlement = Settlement.builder()
                .merchantId(merchantId)
                .grossAmount(gross)
                .feeAmount(feeAmount)
                .gstAmount(gstAmount)
                .netAmount(netAmount)
                .status(SettlementStatus.INITIATED)
                .build();

        settlementRepository.save(settlement);

        try {
            List<SettlementPayment> links = new ArrayList<>();
            for (PaymentSettlementView p : unsettledPayments) {
                links.add(SettlementPayment.builder()
                        .id(new SettlementPaymentId(settlement.getId(), p.paymentId()))
                        .settlement(settlement)
                        .build());
            }
            settlementPaymentRepository.saveAll(links);

            SettlementBankDetails settlementBankDetails = merchantServiceClient.getSettlementBankDetails(merchantId);
            BankTransferResult bankTransferResult = bankTransferProcessor.initiate(settlement.getId(), merchantId, netAmount,
                    settlementBankDetails.accountNumber(), settlementBankDetails.ifsc());

            settlement.setStatus(SettlementStatus.TRANSFER_PENDING);
            settlement.setBankReference(bankTransferResult.registrationRef());

            settlementRepository.save(settlement);
        } catch (Exception e) {
            log.error("Settlement failed for settlementId: {} on date: {}", settlement.getId(), settlementDate, e);
            settlement.setStatus(SettlementStatus.FAILED);
            settlementRepository.save(settlement);
        }
    }

    @Transactional
    public void resolveTransfer(UUID settlementId,
                                String errorCode, String errorDescription) {

        Settlement settlement = settlementRepository.findById(settlementId).orElseThrow(
                () -> new ResourceNotFoundException("Settlement", settlementId));

        if (settlement.getStatus() != SettlementStatus.TRANSFER_PENDING) {
            log.info("Settlement resolved, skipping for id: {}", settlement.getId());
            return;
        }

        if (errorCode == null) { // success
            settlement.setStatus(SettlementStatus.PROCESSED);
            settlement.setProcessedAt(LocalDateTime.now());
            settlementRepository.save(settlement);
            log.info("Settlement processed successfully, settlementId: {}", settlement.getId());
            outboxEventPublisher.publish(EventAggregateType.SETTLEMENT, settlementId,
                    "SETTLEMENT_PROCESSED", Map.of(
                            "settlementId", settlementId,
                            "merchantId", settlement.getMerchantId(),
                            "status", settlement.getStatus().name(),
                            "settlementAmount", settlement.getNetAmount().getAmountUnits(),
                            "settlementCurrency", settlement.getNetAmount().getCurrency()
                    ));
        } else { // failed
            settlement.setStatus(SettlementStatus.FAILED);
            settlement.setFailureReason(errorCode+" : "+errorDescription);
            settlementRepository.save(settlement);
            log.warn("Settlement failed, settlementId: {}", settlement.getId());
            outboxEventPublisher.publish(EventAggregateType.SETTLEMENT, settlementId,
                    "SETTLEMENT_FAILED", Map.of(
                            "settlementId", settlementId,
                            "merchantId", settlement.getMerchantId(),
                            "status", settlement.getStatus().name(),
                            "settlementAmount", settlement.getNetAmount().getAmountUnits(),
                            "settlementCurrency", settlement.getNetAmount().getCurrency()
                    ));
        }

    }



}




















