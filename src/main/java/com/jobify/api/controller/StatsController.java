package com.jobify.api.controller;

import com.jobify.api.dto.stats.StackStatsDTO;
import com.jobify.api.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal stats API — protected by InternalApiKeyFilter.
 * Not publicly exposed. Consumers must supply X-Internal-Api-Key header.
 *
 * Used by the Next.js server layer to populate pSEO landing pages at
 * build time or on-demand, then cached at the CDN level.
 */
@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Stats", description = "Aggregate stats endpoints for pSEO landing pages")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(
            summary = "Stack stats",
            description = "Returns aggregate stats for a given tech stack tag. " +
                          "Results are Redis-cached for 24 hours."
    )
    @GetMapping("/stack")
    public ResponseEntity<StackStatsDTO> getStackStats(
            @RequestParam String tag) {

        if (tag == null || tag.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(statsService.getStackStats(tag.trim()));
    }
}
