package com.toll.verify.api;

import com.toll.verify.service.DltReplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dlt")
@RequiredArgsConstructor
public class DltReplayController {

    private final DltReplayService replayService;

    @PostMapping("/replay/charge")
    public String replayChargeDLT() {
        int count = replayService.replayDLT(
                "toll.charge.request.DLT",
                "toll.charge.request"
        );
        return "✅ Replayed " + count + " messages from toll.charge.request.DLT";
    }

    @PostMapping("/replay/gate")
    public String replayGateDLT() {
        int count = replayService.replayDLT(
                "toll.gate.command.DLT",
                "toll.gate.command"
        );
        return "✅ Replayed " + count + " messages from toll.gate.command.DLT";
    }
}
