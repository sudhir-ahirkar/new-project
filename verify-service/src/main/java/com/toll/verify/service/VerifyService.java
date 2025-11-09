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
        boolean sufficientFunds = prevBalance >= toll;
        String status = sufficientFunds ? "PENDING_PAYMENT" : "INSUFFICIENT_FUNDS";

        // Save the transaction FIRST
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
                .status(status)
                .createdAt(Instant.now())
                .build();

        txRepo.save(tx);
        log.info("Transaction persisted BEFORE publishing: {}", tx);

        // Publish Kafka *after* commit finishes
        TagInfo finalStored = stored;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {

                if (sufficientFunds) {

                    // Publish charge request (always)
                    TagChargeRequest chargeReq = TagChargeRequest.builder()
                            .eventId(eventId)
                            .tagId(finalStored.getTagId())
                            .amount(toll)
                            .timestamp(Instant.now().toString())
                            .build();

                    chargeKafkaTemplate.send(paymentRequestTopic, finalStored.getTagId(), chargeReq);
                    log.info("Published TagChargeRequest AFTER COMMIT: {}", chargeReq);

                    // FEATURE TOGGLE: Open gate now OR wait for payment-success callback
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

                        gateKafkaTemplate.send(
                                "toll.gate.command",
                                gateCmd.getPlazaId() + ":" + gateCmd.getLaneId(),
                                gateCmd
                        );
                        log.info("Gate OPEN published (PAYMENT_REQUESTED mode): {}", gateCmd);

                    } else {
                        log.info("Gate opening deferred — waiting for payment SUCCESS before opening.");
                    }

                } else {

                    // Insufficient funds → straight DENY
                    OpenGateCommand gateCmd = OpenGateCommand.builder()
                            .eventId(eventId)
                            .tagId(finalStored.getTagId())
                            .plazaId(incoming.getCurrentTrip().getPlazaId())
                            .laneId(incoming.getCurrentTrip().getLaneId())
                            .decision(Decision.DENY)
                            .reason("INSUFFICIENT_FUNDS")
                            .timestamp(Instant.now())
                            .build();

                    gateKafkaTemplate.send(
                            "toll.gate.command",
                            gateCmd.getPlazaId() + ":" + gateCmd.getLaneId(),
                            gateCmd
                    );

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
            log.warn("No matching transaction found for eventId={}, ignoring payment response", resp.getEventId());
            return;
        }

        TollTransaction tx = optTx.get();

        // Load TagInfo from Redis
        String redisKey = "TAG:" + tx.getTagId();
        TagInfo stored = tagRedisTemplate.opsForValue().get(redisKey);

        if (stored == null) {
            log.error("Redis missing tag info for {}, cannot finalize.", tx.getTagId());
            return;
        }

        // SUCCESS CASE
        if (resp.getStatus() == ChargeStatus.SUCCESS) {

            tx.setStatus("SUCCESS");
            // Balance was already deducted in process() — so nothing to revert

            // OPEN gate
            OpenGateCommand gateCmd = OpenGateCommand.builder()
                    .eventId(resp.getEventId())
                    .tagId(tx.getTagId())
                    .plazaId(tx.getPlazaId())
                    .laneId(tx.getLaneId())
                    .decision(Decision.OPEN)
                    .reason("PAID")
                    .timestamp(Instant.now())
                    .build();

            publishGateCommand(gateCmd);
            log.info("Payment SUCCESS — Gate OPEN issued: {}", gateCmd);

        }
        // FAILED CASE — FULL BLACKLIST LOGIC
        else {

            // revert previous deduction
            stored.setBalance(tx.getPreviousBalance());
            tagRedisTemplate.opsForValue().set(redisKey, stored, cacheTtlMinutes, TimeUnit.MINUTES);

            tx.setStatus("FAILED");

            // 🆕 Blacklist for 24 hours
            BlacklistEntry entry = BlacklistEntry.builder()
                    .tagId(tx.getTagId())
                    .reason("PAYMENT_FAILED")
                    .timestamp(Instant.now())
                    .build();

            blacklistRedisTemplate.opsForValue().set(
                    "BLACKLIST:" + tx.getTagId(),
                    entry,
                    24, TimeUnit.HOURS
            );

            log.warn("Tag {} blacklisted for 24h due to payment failure.", tx.getTagId());

            // DENY gate command
            OpenGateCommand gateCmd = OpenGateCommand.builder()
                    .eventId(resp.getEventId())
                    .tagId(tx.getTagId())
                    .plazaId(tx.getPlazaId())
                    .laneId(tx.getLaneId())
                    .decision(Decision.DENY)
                    .reason("PAYMENT_FAILED")
                    .timestamp(Instant.now())
                    .build();

            publishGateCommand(gateCmd);
            log.warn("Payment FAILED — Gate DENY issued: {}", gateCmd);
        }

        txRepo.save(tx);
        log.info("💾 Transaction updated eventId={} finalStatus={}", tx.getEventId(), tx.getStatus());
    }
}
