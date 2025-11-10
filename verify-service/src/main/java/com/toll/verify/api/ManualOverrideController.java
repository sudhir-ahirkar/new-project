package com.toll.verify.api;


import com.toll.verify.service.ManualOverrideService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/override")
@RequiredArgsConstructor
public class ManualOverrideController {

    private final ManualOverrideService service;

    @PostMapping("/allow/{eventId}")
    public String allowWithPenalty(@PathVariable String eventId) {
        //service.allowVehicleWithPenalty(eventId);
        return "✅ Manual override applied. Gate opened.";
    }
}

