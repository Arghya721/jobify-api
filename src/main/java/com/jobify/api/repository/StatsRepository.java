package com.jobify.api.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.jobify.api.dto.stats.*;

import java.util.List;

/**
 * Native SQL queries for stack stats. Uses JdbcTemplate to avoid entity mapping
 * overhead and allow efficient aggregate queries across job_details.
 *
 * Tag matching uses the structured job_details.tags text[] column (populated by
 * the job-processor skill extractor) instead of a LIKE scan over raw_description.
 * Membership is case-insensitive via unnest so callers can pass any casing;
 * the canonical tags are produced by the extractor dictionary.
 */
@Repository
public class StatsRepository {

    private final JdbcTemplate jdbc;

    public StatsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    /**
     * SQL predicate: true when job_details row {@code jd} carries the given tag
     * (case-insensitive). Bind one String parameter — the tag.
     */
    // Leading space is required: Java text blocks strip trailing whitespace per
    // line, so the preceding "AND"/"WHERE" would otherwise glue to "EXISTS".
    private static final String HAS_TAG =
            " EXISTS (SELECT 1 FROM unnest(jd.tags) AS tg WHERE lower(tg) = lower(?))";

    // ─── Total active jobs mentioning this tag ──────────────────────────────

    public long countTotalJobs(String tag) {
        String sql = """
                SELECT COUNT(*)
                FROM jobs j
                JOIN job_details jd ON j.id = jd.job_id
                WHERE j.is_active = true
                  AND """ + HAS_TAG + """
                """;
        Long count = jdbc.queryForObject(sql, Long.class, tag);
        return count != null ? count : 0L;
    }

    // ─── Co-occurring skills ─────────────────────────────────────────────────

    public List<CoOccurringSkillDTO> findCoOccurringSkills(String tag) {
        // Subquery for denominator (total jobs with tag) avoids a separate query
        String sql = """
                SELECT tags.tag,
                       COUNT(*) AS mentions,
                       ROUND(COUNT(*) * 100.0 / NULLIF((
                           SELECT COUNT(*)
                           FROM jobs jj JOIN job_details jd ON jj.id = jd.job_id
                           WHERE jj.is_active = true AND """ + HAS_TAG + """
                       ), 0)) AS pct
                FROM (VALUES
                    ('AWS'), ('Docker'), ('Kubernetes'), ('PostgreSQL'), ('Redis'),
                    ('FastAPI'), ('Django'), ('TypeScript'), ('React'), ('Terraform'),
                    ('Spark'), ('Airflow'), ('GraphQL'), ('Go'), ('Rust'), ('Node.js'),
                    ('Next.js'), ('MongoDB'), ('Elasticsearch'), ('Kafka')
                ) AS tags(tag)
                JOIN job_details jd
                  ON EXISTS (SELECT 1 FROM unnest(jd.tags) AS tg WHERE lower(tg) = lower(tags.tag))
                JOIN jobs j ON j.id = jd.job_id
                WHERE j.is_active = true
                  AND """ + HAS_TAG + """
                  AND lower(tags.tag) != lower(?)
                GROUP BY tags.tag
                ORDER BY mentions DESC
                LIMIT 8
                """;
        return jdbc.query(sql,
                (rs, row) -> new CoOccurringSkillDTO(
                        rs.getString("tag"),
                        rs.getLong("mentions"),
                        rs.getLong("pct")),
                tag, tag, tag);
    }

    // ─── Experience distribution ─────────────────────────────────────────────

    public List<ExperienceDistributionDTO> findExperienceDistribution(String tag) {
        String sql = """
                SELECT
                    CASE
                        WHEN jd.experience_min <= 2 THEN 'Entry (0–2 yrs)'
                        WHEN jd.experience_min <= 5 THEN 'Mid (3–5 yrs)'
                        WHEN jd.experience_min <= 8 THEN 'Senior (6–8 yrs)'
                        ELSE 'Staff+ (8+ yrs)'
                    END AS band,
                    COUNT(*) AS count
                FROM jobs j
                JOIN job_details jd ON j.id = jd.job_id
                WHERE j.is_active = true
                  AND """ + HAS_TAG + """
                  AND jd.experience_min IS NOT NULL
                GROUP BY band
                ORDER BY MIN(jd.experience_min)
                """;
        return jdbc.query(sql,
                (rs, row) -> new ExperienceDistributionDTO(
                        rs.getString("band"),
                        rs.getLong("count")),
                tag);
    }

    // ─── Posting velocity (this week vs last week) ───────────────────────────

    public PostingVelocityDTO findPostingVelocity(String tag) {
        String sql = """
                SELECT
                    COUNT(*) FILTER (WHERE jd.job_posted_at >= NOW() - INTERVAL '7 days')  AS this_week,
                    COUNT(*) FILTER (WHERE jd.job_posted_at >= NOW() - INTERVAL '14 days'
                                      AND jd.job_posted_at <  NOW() - INTERVAL '7 days')   AS last_week
                FROM jobs j
                JOIN job_details jd ON j.id = jd.job_id
                WHERE """ + HAS_TAG + """
                """;
        return jdbc.queryForObject(sql, (rs, row) -> {
            long thisWeek = rs.getLong("this_week");
            long lastWeek = rs.getLong("last_week");
            long change = lastWeek == 0 ? 0
                    : Math.round((thisWeek - lastWeek) * 100.0 / lastWeek);
            return new PostingVelocityDTO(thisWeek, lastWeek, change);
        }, tag);
    }

    // ─── Top hiring companies ────────────────────────────────────────────────

    public List<TopCompanyDTO> findTopCompanies(String tag) {
        String sql = """
                SELECT c.name, COUNT(*) AS open_roles
                FROM jobs j
                JOIN companies c  ON j.company_id = c.id
                JOIN job_details jd ON j.id = jd.job_id
                WHERE j.is_active = true
                  AND """ + HAS_TAG + """
                GROUP BY c.id, c.name
                ORDER BY open_roles DESC
                LIMIT 6
                """;
        return jdbc.query(sql,
                (rs, row) -> new TopCompanyDTO(
                        rs.getString("name"),
                        rs.getLong("open_roles")),
                tag);
    }

    // ─── Remote vs onsite breakdown ──────────────────────────────────────────

    public RemoteBreakdownDTO findRemoteBreakdown(String tag) {
        String sql = """
                SELECT
                    COUNT(*) FILTER (WHERE jl.is_remote = true)  AS remote,
                    COUNT(*) FILTER (WHERE jl.is_remote = false) AS onsite,
                    COUNT(*)                                       AS total
                FROM jobs j
                JOIN job_details jd  ON j.id = jd.job_id
                JOIN job_locations jl ON j.id = jl.job_id
                WHERE j.is_active = true
                  AND """ + HAS_TAG + """
                """;
        return jdbc.queryForObject(sql,
                (rs, row) -> new RemoteBreakdownDTO(
                        rs.getLong("remote"),
                        rs.getLong("onsite"),
                        rs.getLong("total")),
                tag);
    }

    // ─── ATS source breakdown ────────────────────────────────────────────────

    public List<AtsBreakdownDTO> findAtsBreakdown(String tag) {
        String sql = """
                SELECT j.job_source AS source, COUNT(*) AS count
                FROM jobs j
                JOIN job_details jd ON j.id = jd.job_id
                WHERE j.is_active = true
                  AND """ + HAS_TAG + """
                GROUP BY j.job_source
                ORDER BY count DESC
                """;
        return jdbc.query(sql,
                (rs, row) -> new AtsBreakdownDTO(
                        rs.getString("source"),
                        rs.getLong("count")),
                tag);
    }
}
