package com.toll.verify.api;

import com.toll.verify.service.VerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manual")
@RequiredArgsConstructor
public class ManualCollectionController {

    private final VerifyService verifyService;

    /**
     * Operator collects toll manually and opens gate
     */
    @PostMapping("/collect/{eventId}")
    public String collect(@PathVariable String eventId,
                          @RequestParam(defaultValue = "true") boolean applyPenalty) {

        double penaltyMultiplier = applyPenalty ? 2.0 : 1.0;
        verifyService.handleManualCollection(eventId, penaltyMultiplier);

        return "Manual toll processed for eventId=" + eventId +
                (applyPenalty ? " (Penalty Applied)" : "");
    }
}
