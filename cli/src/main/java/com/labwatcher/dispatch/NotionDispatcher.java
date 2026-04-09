package com.labwatcher.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labwatcher.format.NotionFormatter;
import com.labwatcher.model.FileSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** POSTs a {@link FileSummary} to the Notion API as a new database row. */
public final class NotionDispatcher implements Dispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(NotionDispatcher.class);
    private static final String ENDPOINT = "https://api.notion.com/v1/pages";
    private static final String NOTION_VERSION = "2022-06-28";

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final NotionFormatter formatter = new NotionFormatter();
    private final String token;
    private final String databaseId;

    public NotionDispatcher(String token, String databaseId) {
        this(token, databaseId,
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    /** Test seam: inject a custom HttpClient. */
    public NotionDispatcher(String token, String databaseId, HttpClient http) {
        this.token = token;
        this.databaseId = databaseId;
        this.http = http;
    }

    @Override public String name() { return "notion"; }

    @Override
    public boolean dispatch(FileSummary summary) {
        if (token.isEmpty() || databaseId.isEmpty()) {
            LOG.warn("notion: token or database_id missing, skipping");
            return false;
        }
        try {
            String body = mapper.writeValueAsString(
                formatter.buildPageBody(databaseId, summary));
            HttpRequest req = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .header("Authorization", "Bearer " + token)
                .header("Notion-Version", NOTION_VERSION)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 == 2) {
                LOG.info("notion: created page for {}", summary.fileName());
                return true;
            }
            LOG.error("notion: HTTP {} body={}", resp.statusCode(), resp.body());
            return false;
        } catch (Exception e) {
            LOG.error("notion: dispatch failed: {}", e.getMessage());
            return false;
        }
    }
}
