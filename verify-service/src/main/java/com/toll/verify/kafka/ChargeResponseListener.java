package com.toll.verify.kafka;

import com.toll.common.model.TagChargeResponse;
import com.toll.verify.service.VerifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChargeResponseListener {

    private final VerifyService verifyService;

    @Transactional
    @KafkaListener(
            topics = "${payment.topics.response}",
            containerFactory = "chargeResponseListenerFactory"
    )
    public void handlePaymentResponse(TagChargeResponse response) {

        log.info("Received TagChargeResponse eventId={} status={}",
                response.getEventId(), response.getStatus());

        // Centralize all business logic
        verifyService.applyPaymentResult(response);
    }
}
