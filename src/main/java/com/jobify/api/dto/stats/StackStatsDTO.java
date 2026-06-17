package com.jobify.api.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StackStatsDTO {
    private String tag;
    private long totalJobs;
    private List<CoOccurringSkillDTO> coOccurringSkills;
    private List<ExperienceDistributionDTO> experienceDistribution;
    private PostingVelocityDTO postingVelocity;
    private List<TopCompanyDTO> topCompanies;
    private RemoteBreakdownDTO remoteBreakdown;
    private List<AtsBreakdownDTO> atsBreakdown;
}
