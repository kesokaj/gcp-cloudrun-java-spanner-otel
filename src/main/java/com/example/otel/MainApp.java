package com.example.otel;

import io.opentelemetry.api.OpenTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main application to demonstrate OpenTelemetry with Google Cloud Spanner.
 */
public class MainApp {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) throws IOException {
        logger.info("Initializing OpenTelemetry SDK...");
        OpenTelemetry openTelemetry = OtelConfig.initialize();
        logger.info("SDK Initialized.");

        // --- Configuration: Replace with your Spanner details ---
        // These can also be read from environment variables or a config file.
        String projectId = System.getenv("GCP_PROJECT");
        String spannerInstanceId = System.getenv("SPANNER_INSTANCE_ID");
        String spannerDatabaseId = System.getenv("SPANNER_DATABASE_ID");

        if (projectId == null || spannerInstanceId == null || spannerDatabaseId == null) {
            logger.error("Please set the GCP_PROJECT, SPANNER_INSTANCE_ID, and SPANNER_DATABASE_ID environment variables.");
            System.exit(1);
        }

        SpannerService spannerService = new SpannerService(openTelemetry, projectId, spannerInstanceId, spannerDatabaseId);

        logger.info("Starting web server...");
        WebServer webServer = new WebServer(spannerService, openTelemetry);
        webServer.start();
        logger.info("Web server started on port 8080.");
    }
}
