package com.toll.verify.kafka;

import com.toll.verify.entity.TollTransaction;
import com.toll.common.model.ChargeStatus;
import com.toll.common.model.TagChargeResponse;
import com.toll.verify.repository.TollTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChargeResponseListener {

    private final TollTransactionRepository txRepo;

    @Transactional
    @KafkaListener(
            topics = "${payment.topics.response}",
            containerFactory = "responseKafkaListenerFactory"
    )
    public void onResponse(TagChargeResponse resp) {
        log.info("Received charge response eventId={} status={}", resp.getEventId(), resp.getStatus());
        Optional<TollTransaction> opt = txRepo.findByEventId(resp.getEventId());
        if (opt.isEmpty()) {
            log.warn("No pending tx found for eventId={}, ignoring", resp.getEventId());
            return;
        }
        TollTransaction tx = opt.get();

        if (resp.getStatus() == ChargeStatus.SUCCESS) {
            tx.setStatus("SUCCESS");
            // newBalance was already computed/persisted as (prev - toll) in request phase
        } else {
            tx.setStatus("FAILED");
            tx.setNewBalance(tx.getPreviousBalance()); // revert
        }
        txRepo.save(tx);
        log.info("Tx updated eventId={} finalStatus={} newBalance={}",
                tx.getEventId(), tx.getStatus(), tx.getNewBalance());
        // (optional) publish gate-open if SUCCESS
    }
}
