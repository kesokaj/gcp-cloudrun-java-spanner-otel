package com.example.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

/**
 * Configures and initializes the OpenTelemetry SDK.
 * This class uses the autoconfigure module to automatically set up the SDK
 * based on environment variables and system properties.
 */
public class OtelConfig {

    /**
     * Initializes the OpenTelemetry SDK and registers it globally.
     * The SDK is configured using environment variables. See the README for details.
     * For example:
     * - OTEL_SERVICE_NAME=java-spanner-demo
     * - OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
     * - OTEL_METRICS_EXPORTER=otlp
     * - OTEL_LOGS_EXPORTER=otlp
     *
     * @return A configured OpenTelemetry instance.
     */
    public static OpenTelemetrySdk initialize() {
        OpenTelemetrySdk sdk = AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();

        // Add a shutdown hook to ensure telemetry is flushed before the application exits.
        Runtime.getRuntime().addShutdownHook(new Thread(sdk::close));
        
        return sdk;
    }
}
