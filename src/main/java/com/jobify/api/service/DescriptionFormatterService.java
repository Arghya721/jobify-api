package com.jobify.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

/**
 * Rebuilds a clean, structured HTML job description from the raw provider
 * payload stored in jobs.metadata. Each ATS (workday/greenhouse/lever/eightfold)
 * embeds the description differently, so extraction is source-specific; the
 * result is then run through a strict tag allowlist before it's safe to render.
 */
@Service
public class DescriptionFormatterService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Safelist SAFELIST = Safelist.none()
            .addTags("p", "br", "b", "strong", "i", "em", "u", "ul", "ol", "li",
                    "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "a",
                    "table", "thead", "tbody", "tr", "td", "th")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https")
            .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer")
            .addEnforcedAttribute("a", "target", "_blank");

    /** Extracts and sanitizes the description HTML for a job, per its source ATS. */
    public String toHtml(String jobSource, String metadataJson) {
        if (jobSource == null || metadataJson == null || metadataJson.isBlank()) {
            return "";
        }

        String rawHtml;
        try {
            JsonNode root = MAPPER.readTree(metadataJson);
            rawHtml = switch (jobSource) {
                case "workday", "eightfold" -> root.path("jobDescription").asText("");
                case "greenhouse" -> Parser.unescapeEntities(root.path("content").asText(""), false);
                case "lever" -> extractLever(root);
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }

        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }

        return Jsoup.clean(rawHtml, "", SAFELIST, new Document.OutputSettings().prettyPrint(false));
    }

    private String extractLever(JsonNode root) {
        JsonNode lists = root.path("lists");
        if (!lists.isArray() || lists.isEmpty()) {
            return root.path("descriptionPlain").asText("");
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode item : lists) {
            String heading = item.path("text").asText("");
            String content = item.path("content").asText("");
            if (!heading.isBlank()) {
                builder.append("<h3>").append(Parser.unescapeEntities(heading, false)).append("</h3>");
            }
            builder.append(content);
        }
        return builder.toString();
    }
}
