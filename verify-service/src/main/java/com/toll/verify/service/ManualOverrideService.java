package com.toll.verify.service;

import com.toll.common.model.TagInfo;
import com.toll.verify.entity.TollTransaction;
import com.toll.verify.repository.TollTransactionRepository;
import com.toll.common.model.Decision;
import com.toll.common.model.OpenGateCommand;
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
public class ManualOverrideService {

//    private final TollTransactionRepository txRepo;
//    private final RedisTemplate<String, Object> redisTemplate;
//    private final VerifyService verifyService;

/*    @Transactional
    public void allowVehicleWithPenalty(String eventId) {

        Optional<TollTransaction> opt = txRepo.findByEventId(eventId);
        if (opt.isEmpty()) {
            log.warn("No transaction found for eventId={} - cannot override.", eventId);
            return;
        }

        TollTransaction tx = opt.get();
        double toll = tx.getTollAmount();
        double penalty; // 100% extra
        penalty = toll;
        double total = toll + penalty;

        tx.setStatus("MANUAL_OVERRIDE");
        tx.setNewBalance(tx.getPreviousBalance() - total);
        txRepo.save(tx);

        // Send OPEN command
        OpenGateCommand cmd = OpenGateCommand.builder()
                .eventId(eventId)
                .tagId(tx.getTagId())
                .plazaId(tx.getPlazaId())
                .laneId(tx.getLaneId())
                .decision(Decision.OPEN)
                .reason("MANUAL_OVERRIDE_PENALTY=" + penalty)
                .timestamp(Instant.now())
                .build();

        verifyService.publishGateCommand(cmd);

        log.info("Manual override executed for eventId={} totalCharged={}", eventId, total);
    }*/


 /*   @Transactional
    public void handleManualCollection(String eventId, double toll, double penalty) {

        // 1) Fetch Tx
        TollTransaction tx = txRepo.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("No transaction found for " + eventId));

        log.warn("Manual toll collection requested for eventId={} (toll={}, penalty={})",
                eventId, toll, penalty);

        // 2) Restore original balance (no digital deduction)
        String redisKey = "TAG:" + tx.getTagId();
        TagInfo stored = redisTemplate.opsForValue().get(redisKey);

        if (stored != null) {
            // ensure no deduction
            stored.setBalance(tx.getPreviousBalance());
            tagRedisTemplate.opsForValue().set(redisKey, stored, cacheTtlMinutes, TimeUnit.MINUTES);
            log.info("Restored balance in Redis → tag={} balance={}", stored.getTagId(), stored.getBalance());
        }

        // 3) Update transaction record
        tx.setStatus("MANUAL_COLLECTED");      // New final status
        tx.setManualPenaltyAmount(penalty);     // Record penalty amount
        tx.setNewBalance(tx.getPreviousBalance()); // Balance remains unchanged

        txRepo.save(tx);
        log.info("Updated Tx (manual): eventId={} status=MANUAL_COLLECTED penalty={}",
                eventId, penalty);

        // 4) Now publish OPEN gate command (manual override)
        OpenGateCommand cmd = OpenGateCommand.builder()
                .eventId(tx.getEventId())
                .tagId(tx.getTagId())
                .plazaId(tx.getPlazaId())
                .laneId(tx.getLaneId())
                .decision(Decision.OPEN)
                .reason("MANUAL_COLLECTED")   //Recorded reason for audit
                .timestamp(Instant.now())
                .build();

        verifyService.publishGateCommand(cmd);

        log.warn("Gate OPENED MANUALLY eventId={} reason=MANUAL_COLLECTED", eventId);
    }*/

}
