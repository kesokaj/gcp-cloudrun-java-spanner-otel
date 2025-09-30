# Use a specific version of the official OpenTelemetry Collector Contrib image.
# Using the "contrib" version includes the Google Cloud exporter.
FROM otel/opentelemetry-collector-contrib:0.90.1

# Copy your custom configuration file into the image.
# The collector will be configured to look for this file at this path.
COPY otel-config.yaml /etc/otelcol-contrib/config.yaml