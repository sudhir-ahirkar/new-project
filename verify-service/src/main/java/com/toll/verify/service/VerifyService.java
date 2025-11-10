package com.toll.verify.service;

import com.toll.common.model.*;
import com.toll.verify.entity.TollTransaction;
import com.toll.verify.repository.TollTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyService {

    private final TollTransactionRepository txRepo;
    private final RedisTemplate<String, TagInfo> tagRedisTemplate;
    private final RedisTemplate<String, BlacklistEntry> blacklistRedisTemplate;
    private final TagVendorClient vendorClient;
    private final KafkaTemplate<String, OpenGateCommand> gateKafkaTemplate;
    private final KafkaTemplate<String, TagChargeRequest> chargeKafkaTemplate;

    @Value("${payment.topics.request}")
    private String paymentRequestTopic;

    @Value("${cache.ttl-minutes:5}")
    private long cacheTtlMinutes;

    @Value("${verify.open-gate-on-request:true}")
    private boolean openGateOnRequest;

    @Transactional
    public void process(TagInfo incoming) {

        String eventId = incoming.getTagId() + "-" + incoming.getCurrentTrip().getTimestamp();

        // Idempotency check
        if (txRepo.findByEventId(eventId).isPresent()) {
            log.info("Skipping duplicate event {}", eventId);
            return;
        }

        String redisKey = "TAG:" + incoming.getTagId();
        TagInfo stored = tagRedisTemplate.opsForValue().get(redisKey);

        if (stored == null) {
            stored = vendorClient.fetchTag(incoming.getTagId());
            if (stored == null) return;
            tagRedisTemplate.opsForValue().set(redisKey, stored, cacheTtlMinutes, TimeUnit.MINUTES);
        }

        double toll = incoming.getCurrentTrip().getTollAmount();
        double prevBalance = stored.getBalance();

        // ✅ NEW: Manual override flow (Blacklisted, Manual Required, etc.)
        if ("MANUAL_REQUIRED".equalsIgnoreCase(incoming.getCurrentTrip().getStatus())) {
            log.warn("🚫 Tag {} flagged MANUAL_REQUIRED — sending gate DENY and recording transaction",
                    incoming.getTagId());

            TollTransaction tx = TollTransaction.builder()
                    .eventId(eventId)
                    .tagId(stored.getTagId())
                    .vehicleNumber(stored.getVehicleNumber())
                    .vehicleType(stored.getVehicleType())
                    .plazaId(incoming.getCurrentTrip().getPlazaId())
                    .laneId(incoming.getCurrentTrip().getLaneId())
                    .timestamp(Instant.now())
                    .tollAmount(toll)
                    .previousBalance(prevBalance)
                    .newBalance(prevBalance)
                    .status("MANUAL_REQUIRED")
                    .createdAt(Instant.now())
                    .build();

            txRepo.save(tx);

            // Send DENY to gate
            OpenGateCommand gateCmd = OpenGateCommand.builder()
                    .eventId(eventId)
                    .tagId(stored.getTagId())
                    .plazaId(incoming.getCurrentTrip().getPlazaId())
                    .laneId(incoming.getCurrentTrip().getLaneId())
                    .decision(Decision.DENY)
                    .reason("MANUAL_REQUIRED")
                    .timestamp(Instant.now())
                    .build();

            publishGateCommand(gateCmd);

            log.warn("Gate DENY published due to MANUAL_REQUIRED: {}", gateCmd);
            return; // ✅ stop further automatic processing
        }

        // ✅ Normal flow begins here
        boolean sufficientFunds = prevBalance >= toll;
        String status = sufficientFunds ? "PENDING_PAYMENT" : "INSUFFICIENT_FUNDS";

        TollTransaction tx = TollTransaction.builder()
                .eventId(eventId)
                .tagId(stored.getTagId())
                .vehicleNumber(stored.getVehicleNumber())
                .vehicleType(stored.getVehicleType())
                .plazaId(incoming.getCurrentTrip().getPlazaId())
                .laneId(incoming.getCurrentTrip().getLaneId())
                .timestamp(Instant.now())
                .tollAmount(toll)
                .previousBalance(prevBalance)
                .newBalance(prevBalance) // will update on SUCCESS later
                .status(status)
                .createdAt(Instant.now())
                .build();

        txRepo.save(tx);
        log.info("Transaction persisted BEFORE publishing: {}", tx);

        TagInfo finalStored = stored;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {

                if (sufficientFunds) {

                    // Publish charge request
                    TagChargeRequest chargeReq = TagChargeRequest.builder()
                            .eventId(eventId)
                            .tagId(finalStored.getTagId())
                            .amount(toll)
                            .timestamp(Instant.now().toString())
                            .build();

                    chargeKafkaTemplate.send(paymentRequestTopic, finalStored.getTagId(), chargeReq);
                    log.info("Published TagChargeRequest AFTER COMMIT: {}", chargeReq);

                    // Feature toggle: open now or wait for payment confirmation
                    if (openGateOnRequest) {
                        OpenGateCommand gateCmd = OpenGateCommand.builder()
                                .eventId(eventId)
                                .tagId(finalStored.getTagId())
                                .plazaId(incoming.getCurrentTrip().getPlazaId())
                                .laneId(incoming.getCurrentTrip().getLaneId())
                                .decision(Decision.OPEN)
                                .reason("PAYMENT_REQUESTED")
                                .timestamp(Instant.now())
                                .build();

                        publishGateCommand(gateCmd);
                        log.info("Gate OPEN published (PAYMENT_REQUESTED mode): {}", gateCmd);
                    }

                } else {
                    // Insufficient funds → deny immediately
                    OpenGateCommand gateCmd = OpenGateCommand.builder()
                            .eventId(eventId)
                            .tagId(finalStored.getTagId())
                            .plazaId(incoming.getCurrentTrip().getPlazaId())
                            .laneId(incoming.getCurrentTrip().getLaneId())
                            .decision(Decision.DENY)
                            .reason("INSUFFICIENT_FUNDS")
                            .timestamp(Instant.now())
                            .build();

                    publishGateCommand(gateCmd);
                    log.warn("Gate DENY published AFTER COMMIT: {}", gateCmd);
                }
            }
        });
    }


    /**
     * Publishes a gate command event to Kafka.
     * This is called only after final payment confirmation.
     */
    public void publishGateCommand(OpenGateCommand cmd) {
        String routingKey = cmd.getPlazaId() + ":" + cmd.getLaneId(); // Partition by lane for scaling
        gateKafkaTemplate.send("toll.gate.command", routingKey, cmd);
        log.info("🚦 GateCommand published to topic=toll.gate.command routingKey={} cmd={}", routingKey, cmd);
    }

    @Transactional
    public void applyPaymentResult(TagChargeResponse resp) {

        log.info("Applying payment result for eventId={} status={}", resp.getEventId(), resp.getStatus());

        Optional<TollTransaction> optTx = txRepo.findByEventId(resp.getEventId());
        if (optTx.isEmpty()) {
            log.warn("No matching transaction found for eventId={}, ignoring", resp.getEventId());
            return;
        }

        TollTransaction tx = optTx.get();

        // Load Redis Tag Data
        String redisKey = "TAG:" + tx.getTagId();
        TagInfo stored = tagRedisTemplate.opsForValue().get(redisKey);

        if (stored == null) {
            log.error("Redis missing TagInfo for {}", tx.getTagId());
            return;
        }

        // ✅ SUCCESS CASE → Deduct Balance Here
        if (resp.getStatus() == ChargeStatus.SUCCESS) {

            double prevBalance = tx.getPreviousBalance();
            double toll = tx.getTollAmount();
            double newBalance = prevBalance - toll;

            // ✅ Update Redis
            stored.setBalance(newBalance);
            tagRedisTemplate.opsForValue().set(redisKey, stored, cacheTtlMinutes, TimeUnit.MINUTES);

            // ✅ Update DB
            tx.setNewBalance(newBalance);
            tx.setStatus("SUCCESS");
            txRepo.save(tx);

            // ✅ OPEN Gate
            OpenGateCommand gateCmd = OpenGateCommand.builder()
                    .eventId(tx.getEventId())
                    .tagId(tx.getTagId())
                    .plazaId(tx.getPlazaId())
                    .laneId(tx.getLaneId())
                    .decision(Decision.OPEN)
                    .reason("PAID")
                    .timestamp(Instant.now())
                    .build();

            publishGateCommand(gateCmd);

            log.info("✅ Payment SUCCESS → Balance deducted {} → {} and gate opened.", prevBalance, newBalance);
            return;
        }

        // ❌ FAILED CASE → Revert & Blacklist
        stored.setBalance(tx.getPreviousBalance());
        tagRedisTemplate.opsForValue().set(redisKey, stored, cacheTtlMinutes, TimeUnit.MINUTES);

        tx.setStatus("FAILED");
        txRepo.save(tx);

        BlacklistEntry entry = BlacklistEntry.builder()
                .tagId(tx.getTagId())
                .reason("PAYMENT_FAILED")
                .timestamp(Instant.now())
                .build();

        blacklistRedisTemplate.opsForValue().set("BLACKLIST:" + tx.getTagId(), entry, 24, TimeUnit.HOURS);

        OpenGateCommand denyCmd = OpenGateCommand.builder()
                .eventId(tx.getEventId())
                .tagId(tx.getTagId())
                .plazaId(tx.getPlazaId())
                .laneId(tx.getLaneId())
                .decision(Decision.DENY)
                .reason("PAYMENT_FAILED")
                .timestamp(Instant.now())
                .build();

        publishGateCommand(denyCmd);

        log.warn("🚫 Payment FAILED → Blacklisted tag {} and gate denied.", tx.getTagId());
    }

    @Transactional
    public void handleManualCollection(String eventId, double penaltyMultiplier) {

        TollTransaction tx = txRepo.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("No transaction found for " + eventId));

        double penaltyAmount = tx.getTollAmount() * penaltyMultiplier;

        tx.setStatus("MANUAL_COLLECTED");
        tx.setManualPenaltyAmount(penaltyAmount);
        tx.setNewBalance(tx.getPreviousBalance()); // No balance deduction
        txRepo.save(tx);

        // ✅ Remove from blacklist after manual payment
        blacklistRedisTemplate.delete("BLACKLIST:" + tx.getTagId());

        log.info("💰 Manual toll processed for {} penalty={} newStatus={}", tx.getTagId(), penaltyAmount, tx.getStatus());

        // ✅ Open gate manually now
        OpenGateCommand cmd = OpenGateCommand.builder()
                .eventId(tx.getEventId())
                .tagId(tx.getTagId())
                .plazaId(tx.getPlazaId())
                .laneId(tx.getLaneId())
                .decision(Decision.OPEN)
                .reason("MANUAL_COLLECTED")
                .timestamp(Instant.now())
                .build();

        publishGateCommand(cmd);
        log.info("🚦 Gate OPEN (manual override): {}", cmd);
    }
}
