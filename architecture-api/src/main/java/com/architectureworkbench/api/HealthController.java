package com.architectureworkbench.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class HealthController {
    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "architecture-api",
                "timestamp", Instant.now().toString()
        );
    }
}
