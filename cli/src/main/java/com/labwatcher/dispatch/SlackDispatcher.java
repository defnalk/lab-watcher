package com.labwatcher.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labwatcher.format.SlackFormatter;
import com.labwatcher.model.FileSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** POSTs a {@link FileSummary} to a Slack incoming webhook URL. */
public final class SlackDispatcher implements Dispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(SlackDispatcher.class);

    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SlackFormatter formatter = new SlackFormatter();
    private final String webhookUrl;

    public SlackDispatcher(String webhookUrl) {
        this(webhookUrl,
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    /** Test seam: inject a custom HttpClient. */
    public SlackDispatcher(String webhookUrl, HttpClient http) {
        this.webhookUrl = webhookUrl;
        this.http = http;
    }

    @Override public String name() { return "slack"; }

    @Override
    public boolean dispatch(FileSummary summary) {
        if (webhookUrl.isEmpty()) {
            LOG.warn("slack: webhook url missing, skipping");
            return false;
        }
        try {
            String body = mapper.writeValueAsString(formatter.buildPayload(summary));
            HttpRequest req = HttpRequest.newBuilder(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 == 2) {
                LOG.info("slack: posted summary for {}", summary.fileName());
                return true;
            }
            LOG.error("slack: HTTP {} body={}", resp.statusCode(), resp.body());
            return false;
        } catch (InterruptedException e) {
            // Preserve interrupt status so the surrounding shutdown flow
            // (FileProcessor's executor drain) can observe it and stop.
            Thread.currentThread().interrupt();
            LOG.warn("slack: dispatch interrupted");
            return false;
        } catch (Exception e) {
            LOG.error("slack: dispatch failed: {}", e.getMessage());
            return false;
        }
    }
}
