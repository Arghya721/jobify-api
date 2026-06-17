package com.jobify.api.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoOccurringSkillDTO {
    private String tag;
    private long mentions;
    private long pct;
}
