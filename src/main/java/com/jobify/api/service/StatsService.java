package com.jobify.api.service;

import com.jobify.api.dto.stats.*;
import com.jobify.api.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Computes and caches stack stats. All results are cached in the "stack_stats"
 * Redis cache (TTL = 86400s / 1 day — configured in RedisConfig).
 *
 * Cache key is the normalised lowercase tag so "Python" and "python" hit the
 * same entry.
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsRepository statsRepository;

    @Cacheable(cacheNames = "stack_stats",
            key = "#tag.toLowerCase() + '|' + (#coOccurringTags == null ? '' : T(java.lang.String).join(',', #coOccurringTags).toLowerCase())")
    public StackStatsDTO getStackStats(String tag, List<String> coOccurringTags) {
        String normalised = tag.toLowerCase();

        long totalJobs = statsRepository.countTotalJobs(normalised);

        List<CoOccurringSkillDTO> coOccurring =
                statsRepository.findCoOccurringSkills(normalised, coOccurringTags);

        List<ExperienceDistributionDTO> expDist =
                statsRepository.findExperienceDistribution(normalised);

        PostingVelocityDTO velocity =
                statsRepository.findPostingVelocity(normalised);

        List<TopCompanyDTO> topCompanies =
                statsRepository.findTopCompanies(normalised);

        RemoteBreakdownDTO remote =
                statsRepository.findRemoteBreakdown(normalised);

        List<AtsBreakdownDTO> ats =
                statsRepository.findAtsBreakdown(normalised);

        return new StackStatsDTO(
                tag,
                totalJobs,
                coOccurring,
                expDist,
                velocity,
                topCompanies,
                remote,
                ats
        );
    }
}
