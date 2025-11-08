package com.toll.verify.service;

import com.toll.common.model.Decision;
import com.toll.common.model.OpenGateCommand;
import com.toll.common.model.TagInfo;
import com.toll.verify.entity.TollTransaction;
import com.toll.verify.repository.TollTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyService {

    private final TollTransactionRepository txRepo;
    private final RedisTemplate<String, TagInfo> redisTemplate;
    private final TagVendorClient vendorClient;

    // Kafka producer to publish gate commands
    private final KafkaTemplate<String, OpenGateCommand> gateKafkaTemplate;

    @Value("${cache.ttl-minutes:5}")
    private long cacheTtlMinutes;

    @Transactional
    public void process(TagInfo incoming) {

        // Generate stable event ID
        String eventId = incoming.getCurrentTrip() == null
                ? incoming.getTagId() + "-" + Instant.now().toEpochMilli()
                : incoming.getTagId() + "-" + incoming.getCurrentTrip().getTimestamp();

        // Idempotency check
        if (txRepo.findByEventId(eventId).isPresent()) {
            log.info("Skipping duplicate event {}", eventId);
            return;
        }

        String redisKey = "TAG:" + incoming.getTagId();

        TagInfo stored = redisTemplate.opsForValue().get(redisKey);
        if (stored == null) {
            log.info("Cache miss for {} — fetching from vendor...", incoming.getTagId());
            stored = vendorClient.fetchTag(incoming.getTagId());
            if (stored == null) {
                log.warn("Vendor returned no data for tag {}", incoming.getTagId());
                return;
            }
            redisTemplate.opsForValue().set(redisKey, stored, cacheTtlMinutes, TimeUnit.MINUTES);
        }

        double toll = incoming.getCurrentTrip() != null && incoming.getCurrentTrip().getTollAmount() != null
                ? incoming.getCurrentTrip().getTollAmount()
                : 0.0;

        double prevBalance = stored.getBalance() == null ? 0.0 : stored.getBalance();
        double newBalance = prevBalance;
        String status;

        // SUCCESS PATH — sufficient balance
        if (prevBalance >= toll) {
            newBalance = prevBalance - toll;
            stored.setBalance(newBalance);
            redisTemplate.opsForValue().set(redisKey, stored, cacheTtlMinutes, TimeUnit.MINUTES);
            status = "SUCCESS";

            // NEW: Send OPEN command to gate
            OpenGateCommand gateCmd = OpenGateCommand.builder()
                    .eventId(eventId)
                    .tagId(stored.getTagId())
                    .plazaId(incoming.getCurrentTrip().getPlazaId())
                    .laneId(incoming.getCurrentTrip().getLaneId())
                    .decision(Decision.OPEN)
                    .reason("PAID")
                    .timestamp(Instant.now())
                    .build();

            String routingKey = gateCmd.getPlazaId() + ":" + gateCmd.getLaneId();
            gateKafkaTemplate.send("toll.gate.command", routingKey, gateCmd);
            log.info("Gate OPEN published: {}", gateCmd);

        } else {
            // INSUFFICIENT FUNDS
            status = "INSUFFICIENT_FUNDS";

            OpenGateCommand gateCmd = OpenGateCommand.builder()
                    .eventId(eventId)
                    .tagId(stored.getTagId())
                    .plazaId(incoming.getCurrentTrip().getPlazaId())
                    .laneId(incoming.getCurrentTrip().getLaneId())
                    .decision(Decision.DENY)
                    .reason("INSUFFICIENT_FUNDS")
                    .timestamp(Instant.now())
                    .build();

            String routingKey = gateCmd.getPlazaId() + ":" + gateCmd.getLaneId();
            gateKafkaTemplate.send("toll.gate.command", routingKey, gateCmd);
            log.warn("Gate DENY published: {}", gateCmd);
        }

        // Persist transaction
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
                .newBalance(newBalance)
                .status(status)
                .createdAt(Instant.now())
                .build();

        txRepo.save(tx);
        log.info("Transaction saved: {}", tx);
    }
}

/*package com.toll.verify.service;

import com.toll.common.model.OpenGateCommand;
import com.toll.common.model.TagInfo;
import com.toll.verify.entity.TollTransaction;
import com.toll.verify.kafka.ChargeRequestPublisher;
import com.toll.common.model.TagChargeRequest;
import com.toll.verify.repository.TollTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyService {

    private final TollTransactionRepository txRepo;
    private final RedisTemplate<String, TagInfo> redisTemplate;
    private final TagVendorClient vendorClient;
    private final ChargeRequestPublisher chargePublisher;
    private final KafkaTemplate<String, OpenGateCommand> gateKafkaTemplate;

    @Value("${cache.ttl-minutes:5}")
    private long cacheTtlMinutes;

    *//**
     * Consume TagInfo and publish a charge request.
     * Writes a PENDING transaction immediately, final status comes via ChargeResponseListener.
     *//*
    @Transactional
    public void process(TagInfo incoming) {
        String eventId = (incoming.getCurrentTrip() == null || incoming.getCurrentTrip().getTimestamp() == null)
                ? incoming.getTagId() + "-" + Instant.now().toEpochMilli()
                : incoming.getTagId() + "-" + incoming.getCurrentTrip().getTimestamp();

        // Idempotency: if we already have this eventId, skip
        Optional<TollTransaction> existing = txRepo.findByEventId(eventId);
        if (existing.isPresent()) {
            log.info("Skipping already processed event {}", eventId);
            return;
        }

        String redisKey = "TAG:" + incoming.getTagId();
        TagInfo account = redisTemplate.opsForValue().get(redisKey);
        if (account == null) {
            account = vendorClient.fetchTag(incoming.getTagId());
            if (account != null) {
                redisTemplate.opsForValue().set(redisKey, account, cacheTtlMinutes, TimeUnit.MINUTES);
                log.info("Cached vendor record for {}", account.getTagId());
            } else {
                log.warn("No account for {}, skipping.", incoming.getTagId());
                return;
            }
        }

        Double toll = (incoming.getCurrentTrip() != null) ? incoming.getCurrentTrip().getTollAmount() : null;
        if (toll == null) toll = 0.0;

        double prevBalance = account.getBalance() == null ? 0.0 : account.getBalance();
        double newBalanceIfSuccess = Math.max(0.0, prevBalance - toll);

        // Persist a PENDING tx immediately (so response can find it)
        TollTransaction tx = TollTransaction.builder()
                .eventId(eventId)
                .tagId(account.getTagId())
                .vehicleNumber(account.getVehicleNumber())
                .vehicleType(account.getVehicleType())
                .plazaId(incoming.getCurrentTrip() != null ? incoming.getCurrentTrip().getPlazaId() : null)
                .laneId(incoming.getCurrentTrip() != null ? incoming.getCurrentTrip().getLaneId() : null)
                .timestamp(Instant.now())
                .tollAmount(toll)
                .previousBalance(prevBalance)
                .newBalance(newBalanceIfSuccess) // optimistic; will keep/revert on response
                .status("PENDING")
                .createdAt(Instant.now())
                .build();
        txRepo.save(tx);

        // Publish charge request (async)
        TagChargeRequest req = TagChargeRequest.builder()
                .eventId(eventId)
                .tagId(account.getTagId())
                .vehicleNumber(account.getVehicleNumber())
                .vehicleType(account.getVehicleType())
                .amount(toll)
                .plazaId(tx.getPlazaId())
                .laneId(tx.getLaneId())
                .timestampIso(incoming.getCurrentTrip() != null ? incoming.getCurrentTrip().getTimestamp() : null)
                .build();

        chargePublisher.publish(req);

        log.info("PENDING tx stored & charge request sent. eventId={} tagId={} amount={} prevBal={}",
                eventId, account.getTagId(), toll, prevBalance);

    }
}*/

/*package com.toll.verify.service;

import com.toll.common.model.TagInfo;
import com.toll.verify.entity.TollTransaction;
import com.toll.verify.repository.TollTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyService {

    private final TollTransactionRepository txRepo;
    private final RedisTemplate<String, TagInfo> redisTemplate;
    private final TagVendorClient vendorClient;

    @Value("${cache.ttl-minutes:5}")
    private long cacheTtlMinutes;

    @Transactional
    public void process(TagInfo incoming) {

        // ✅ ALWAYS use eventId from ingest (if provided)
        String eventId = (incoming.getCurrentTrip() != null &&
                incoming.getCurrentTrip().getEventId() != null)
                ? incoming.getCurrentTrip().getEventId()
                : incoming.getTagId() + "-" + Instant.now().toEpochMilli();

        // ✅ Idempotency Check
        if (txRepo.findByEventId(eventId).isPresent()) {
            log.info("Skipping duplicate event {}", eventId);
            return;
        }

        // ✅ Load tag profile from Redis, fallback to vendor
        String redisKey = "TAG:" + incoming.getTagId();
        TagInfo stored = redisTemplate.opsForValue().get(redisKey);

        if (stored == null) {
            log.info("Cache MISS — fetching tag {} from vendor", incoming.getTagId());
            stored = fetchFromVendorSafely(incoming.getTagId());

            if (stored == null) {
                log.warn("Vendor could not provide tag {}; skipping processing", incoming.getTagId());
                return;
            }

            redisTemplate.opsForValue().set(redisKey, stored, cacheTtlMinutes, TimeUnit.MINUTES);
            log.info("Vendor data cached in Redis for tag {}", stored.getTagId());
        }

        // ✅ Use trip info from incoming (NOT from stored)
        double toll = (incoming.getCurrentTrip() != null &&
                incoming.getCurrentTrip().getTollAmount() != null)
                ? incoming.getCurrentTrip().getTollAmount()
                : 0.0;

        String plazaId = (incoming.getCurrentTrip() != null) ? incoming.getCurrentTrip().getPlazaId() : null;
        String laneId  = (incoming.getCurrentTrip() != null) ? incoming.getCurrentTrip().getLaneId()  : null;

        // ✅ Balance evaluation
        double prevBalance = stored.getBalance() != null ? stored.getBalance() : 0.0;
        double newBalance = prevBalance;
        String status;

        if (prevBalance >= toll) {
            newBalance = prevBalance - toll;
            stored.setBalance(newBalance);

            redisTemplate.opsForValue().set(redisKey, stored, cacheTtlMinutes, TimeUnit.MINUTES);
            status = "SUCCESS";
        } else {
            status = "INSUFFICIENT_FUNDS";
        }

        // ✅ Persist Transaction
        TollTransaction tx = TollTransaction.builder()
                .eventId(eventId)
                .tagId(stored.getTagId())
                .vehicleNumber(stored.getVehicleNumber())
                .vehicleType(stored.getVehicleType())
                .plazaId(plazaId)
                .laneId(laneId)
                .timestamp(Instant.now())
                .tollAmount(toll)
                .previousBalance(prevBalance)
                .newBalance(newBalance)
                .status(status)
                .createdAt(Instant.now())
                .build();

        txRepo.save(tx);

        log.info("💳 Tx saved: eventId={} | {} → {} | status={}",
                eventId, prevBalance, newBalance, status);
    }

    *//**
     * Vendor fallback with safe exception handling.
     *//*
    private TagInfo fetchFromVendorSafely(String tagId) {
        try {
            TagInfo tag = vendorClient.fetchTag(tagId);
            if (tag != null) log.info("✅ Vendor returned tag {}", tag.getTagId());
            return tag;
        } catch (Exception e) {
            log.error("❌ Vendor lookup failure for {}: {}", tagId, e.getMessage());
            return null;
        }
    }
}*/
/*@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyService {

    private final TollTransactionRepository txRepo;
    private final RedisTemplate<String, TagInfo> redisTemplate;
    private final TagVendorClient vendorClient;

    // TTL (time-to-live) for Redis cache in minutes, configurable in application.yml
    @Value("${cache.ttl-minutes:5}")
    private long cacheTtlMinutes;

    *//**
     * Process an incoming tag read event.
     *  - Ensures idempotency using eventId
     *  - Fetches TagInfo from Redis or Vendor if not present
     *  - Validates balance and deducts toll
     *  - Persists transaction result
     *//*
    @Transactional
    public void process(TagInfo incoming) {

        // Generate a unique eventId for the transaction
        String eventId = incoming.getCurrentTrip() == null
                ? incoming.getTagId() + "-" + Instant.now().toEpochMilli()
                : incoming.getTagId() + "-" + incoming.getCurrentTrip().getTimestamp();

        // Prevent duplicate processing for same event
        Optional<TollTransaction> existing = txRepo.findByEventId(eventId);
        if (existing.isPresent()) {
            log.info("Skipping already processed event {}", eventId);
            return;
        }

        // Construct Redis cache key
        String redisKey = "TAG:" + incoming.getTagId();

        // Try fetching from Redis first (cache-aside pattern)
        TagInfo stored = redisTemplate.opsForValue().get(redisKey);
        if (stored == null) {
            log.info("Cache miss for tag {} — calling vendor service", incoming.getTagId());
            stored = fetchFromVendorSafely(incoming.getTagId());
            if (stored != null) {
                // Cache for a limited duration
                redisTemplate.opsForValue().set(redisKey, stored, cacheTtlMinutes, TimeUnit.MINUTES);
                log.info("Cached vendor data for tag {}", stored.getTagId());
            } else {
                log.warn("Vendor returned null for tag {}, skipping processing", incoming.getTagId());
                return;
            }
        }

        // Extract toll amount from the incoming trip
        double toll = (incoming.getCurrentTrip() != null && incoming.getCurrentTrip().getTollAmount() != null)
                ? incoming.getCurrentTrip().getTollAmount()
                : 0.0;

        double prevBalance = stored.getBalance() == null ? 0.0 : stored.getBalance();
        String status;
        double newBalance = prevBalance;

        // Validate and deduct balance
        if (prevBalance >= toll) {
            newBalance = prevBalance - toll;
            stored.setBalance(newBalance);
            redisTemplate.opsForValue().set(redisKey, stored, cacheTtlMinutes, TimeUnit.MINUTES);
            status = "SUCCESS";
        } else {
            status = "INSUFFICIENT_FUNDS";
            log.warn("Tag {} has insufficient balance. Required={}, Available={}", stored.getTagId(), toll, prevBalance);
        }

        // Persist transaction record
        TollTransaction tx = TollTransaction.builder()
                .eventId(eventId)
                .tagId(stored.getTagId())
                .vehicleNumber(stored.getVehicleNumber())
                .vehicleType(stored.getVehicleType())
                .plazaId(incoming.getCurrentTrip() != null ? incoming.getCurrentTrip().getPlazaId() : null)
                .laneId(incoming.getCurrentTrip() != null ? incoming.getCurrentTrip().getLaneId() : null)
                .timestamp(Instant.now())
                .tollAmount(toll)
                .previousBalance(prevBalance)
                .newBalance(newBalance)
                .status(status)
                .createdAt(Instant.now())
                .build();

        txRepo.save(tx);
        log.info("Transaction persisted: {} | status={} | prev={} | new={}",
                tx.getEventId(), tx.getStatus(), tx.getPreviousBalance(), tx.getNewBalance());

        // Future enhancement: Publish downstream event (e.g., gate open / audit log)
    }

    *//**
     * Helper method to safely fetch tag info from vendor system.
     *  - Handles nulls and exceptions gracefully.
     *  - Optionally implement retry or circuit breaker here.
     *//*
    private TagInfo fetchFromVendorSafely(String tagId) {
        try {
            TagInfo tag = vendorClient.fetchTag(tagId);
            if (tag != null) {
                log.info("Fetched tag {} from vendor successfully", tagId);
            } else {
                log.warn("Vendor returned no data for tag {}", tagId);
            }
            return tag;
        } catch (Exception e) {
            log.error("Error calling vendor for tag {}: {}", tagId, e.getMessage());
            return null;
        }
    }
}*/

/*import com.toll.common.model.TagInfo;
import com.toll.verify.entity.TollTransaction;
import com.toll.verify.repository.TollTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyService {
    private final TollTransactionRepository txRepo;
    private final RedisTemplate<String, TagInfo> redisTemplate;
    private final TagVendorClient vendorClient;

    @Transactional
    public void process(TagInfo incoming) {
        String eventId = incoming.getCurrentTrip() == null ? incoming.getTagId()+"-"+Instant.now().toEpochMilli() : incoming.getTagId()+"-"+incoming.getCurrentTrip().getTimestamp();
        // use a stable eventId here if available (ingest can set eventId field in TagInfo currentTrip), for now compose:
        // skip if already processed
        Optional<TollTransaction> existing = txRepo.findByEventId(eventId);
        if (existing.isPresent()) {
            log.info("Skipping already processed event {}", eventId);
            return;
        }

        String redisKey = "TAG:" + incoming.getTagId();
        TagInfo stored = redisTemplate.opsForValue().get(redisKey);
        if (stored == null) {
            // fallback to vendor
            stored = vendorClient.fetchTag(incoming.getTagId());
            // cache briefly
            redisTemplate.opsForValue().set(redisKey, stored);
            log.info("Fetched tag info from vendor and cached: {}", stored.getTagId());
        }

        double toll = incoming.getCurrentTrip() != null && incoming.getCurrentTrip().getTollAmount() != null
                ? incoming.getCurrentTrip().getTollAmount()
                : 0.0;

        double prevBalance = stored.getBalance() == null ? 0.0 : stored.getBalance();
        String status;
        double newBalance = prevBalance;

        if (prevBalance >= toll) {
            newBalance = prevBalance - toll;
            stored.setBalance(newBalance);
            redisTemplate.opsForValue().set(redisKey, stored); // update cache
            status = "SUCCESS";
        } else {
            status = "INSUFFICIENT_FUNDS";
        }

        TollTransaction tx = TollTransaction.builder()
                .eventId(eventId)
                .tagId(stored.getTagId())
                .vehicleNumber(stored.getVehicleNumber())
                .vehicleType(stored.getVehicleType())
                .plazaId(incoming.getCurrentTrip() != null ? incoming.getCurrentTrip().getPlazaId() : null)
                .laneId(incoming.getCurrentTrip() != null ? incoming.getCurrentTrip().getLaneId() : null)
                .timestamp(Instant.now())
                .tollAmount(toll)
                .previousBalance(prevBalance)
                .newBalance(newBalance)
                .status(status)
                .createdAt(Instant.now())
                .build();

        txRepo.save(tx);
        log.info("Transaction persisted: {} status={} prev={} new={}", tx.getEventId(), tx.getStatus(), tx.getPreviousBalance(), tx.getNewBalance());
        // further: publish gate-open command, notify reconcile service, etc.
    }
}*/
