package com.jobify.api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DescriptionFormatterServiceTest {

    private final DescriptionFormatterService formatter = new DescriptionFormatterService();

    @Test
    void workday_convertsBoldAndBreaksToHtml() {
        String metadata = """
                {"jobDescription": "<b>Project Role : </b>Custom Software Engineer<br><b>Must have skills : </b>Java<script>alert(1)</script>"}
                """;

        String html = formatter.toHtml("workday", metadata);

        assertThat(html).contains("<b>Project Role : </b>Custom Software Engineer<br><b>Must have skills : </b>Java");
        assertThat(html).doesNotContain("<script>");
    }

    @Test
    void greenhouse_unescapesDoubleEncodedHtmlAndDropsStyleAttrs() {
        String metadata = """
                {"content": "&lt;p style=\\"color:red\\"&gt;&lt;strong&gt;The Company&lt;/strong&gt;&lt;/p&gt;&lt;ul&gt;&lt;li&gt;PyTorch&lt;/li&gt;&lt;/ul&gt;"}
                """;

        String html = formatter.toHtml("greenhouse", metadata);

        assertThat(html).isEqualTo("<p><strong>The Company</strong></p><ul><li>PyTorch</li></ul>");
    }

    @Test
    void lever_wrapsSectionHeadingsAndConcatenatesListContent() {
        String metadata = """
                {"descriptionPlain": "", "lists": [
                  {"text": "About the Role", "content": "<p>We are hiring.</p>"},
                  {"text": "Skills", "content": "<ul><li>Figma</li></ul>"}
                ]}
                """;

        String html = formatter.toHtml("lever", metadata);

        assertThat(html).isEqualTo("<h3>About the Role</h3><p>We are hiring.</p><h3>Skills</h3><ul><li>Figma</li></ul>");
    }

    @Test
    void lever_fallsBackToDescriptionPlainWhenNoLists() {
        String metadata = """
                {"descriptionPlain": "Plain text only", "lists": []}
                """;

        String html = formatter.toHtml("lever", metadata);

        assertThat(html).isEqualTo("Plain text only");
    }

    @Test
    void eightfold_preservesLinksWithSafeAttributesOnly() {
        String metadata = """
                {"jobDescription": "<p>Visit <a href=\\"http://example.com\\" onclick=\\"evil()\\">us</a></p>"}
                """;

        String html = formatter.toHtml("eightfold", metadata);

        assertThat(html).isEqualTo(
                "<p>Visit <a href=\"http://example.com\" rel=\"nofollow noopener noreferrer\" target=\"_blank\">us</a></p>");
    }

    @Test
    void unknownSourceOrBlankMetadata_returnsEmptyString() {
        assertThat(formatter.toHtml("unknown", "{\"jobDescription\": \"x\"}")).isEmpty();
        assertThat(formatter.toHtml("workday", "")).isEmpty();
        assertThat(formatter.toHtml("workday", null)).isEmpty();
    }
}
