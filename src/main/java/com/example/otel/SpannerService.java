package com.example.otel;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service to handle interactions with the Google Cloud Spanner database.
 * It demonstrates custom tracing, metrics, and logging.
 */
public class SpannerService {

    private static final Logger logger = LoggerFactory.getLogger(SpannerService.class);
    private static final Random random = new Random();

    private final DatabaseClient dbClient;
    private final Tracer tracer;
    private final LongCounter singerQueriesCounter;

    /**
     * Constructs the SpannerService.
     *
     * @param openTelemetry The OpenTelemetry instance for instrumentation.
     * @param projectId     Google Cloud Project ID.
     * @param instanceId    Spanner Instance ID.
     * @param databaseId    Spanner Database ID.
     */
    public SpannerService(OpenTelemetry openTelemetry, String projectId, String instanceId, String databaseId) {
        SpannerOptions options = SpannerOptions.newBuilder().setProjectId(projectId).build();
        Spanner spanner = options.getService();
        this.dbClient = spanner.getDatabaseClient(DatabaseId.of(projectId, instanceId, databaseId));

        // Initialize tracer
        this.tracer = openTelemetry.getTracer(SpannerService.class.getName(), "1.0.0");

        // Initialize meter and custom counter metric
        Meter meter = openTelemetry.getMeter(SpannerService.class.getName());
        this.singerQueriesCounter = meter
            .counterBuilder("spanner.queries")
            .setDescription("Counts the number of queries to the Singers table")
            .setUnit("1")
            .build();
    }

    /**
     * Fetches a singer by ID from the Spanner database.
     * This method is manually instrumented to create a custom trace span.
     * @param singerId The ID of the singer to fetch.
     * @return A JSON string with the singer's information, or null if not found.
     */
    public String getSingerById(long singerId) {
        Span span = tracer.spanBuilder("get-singer-by-id").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("app.singer_id", singerId);
            Statement statement = Statement.newBuilder(
                "SELECT SingerId, FirstName, LastName FROM Singers WHERE SingerId = @singerId"
            ).bind("singerId").to(singerId).build();

            try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
                if (resultSet.next()) {
                    String firstName = resultSet.getString("FirstName");
                    String lastName = resultSet.getString("LastName");
                    span.setAttribute("app.singer_name", firstName + " " + lastName);
                    return String.format("{\"SingerId\": %d, \"FirstName\": \"%s\", \"LastName\": \"%s\"}", singerId, firstName, lastName);
                } else {
                    return null;
                }
            }
        } finally {
            span.end();
        }
    }

    /**
     * Fetches all singers from the Spanner database.
     * @return A JSON string with a list of all singers.
     */
    public String getAllSingers() {
        Span span = tracer.spanBuilder("get-all-singers").startSpan();
        try (Scope scope = span.makeCurrent()) {
            StringBuilder json = new StringBuilder("[");
            Statement statement = Statement.of("SELECT SingerId, FirstName, LastName FROM Singers");
            try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
                while (resultSet.next()) {
                    if (json.length() > 1) {
                        json.append(",");
                    }
                    json.append(String.format("{\"SingerId\": %d, \"FirstName\": \"%s\", \"LastName\": \"%s\"}",
                        resultSet.getLong("SingerId"),
                        resultSet.getString("FirstName"),
                        resultSet.getString("LastName")));
                }
            }
            json.append("]");
            return json.toString();
        } finally {
            span.end();
        }
    }

    /**
     * Fetches a random singer from the Spanner database.
     * @return A JSON string with the singer's information, or null if no singers are found.
     */
    public String getRandomSinger() {
        Span span = tracer.spanBuilder("get-random-singer").startSpan();
        try (Scope scope = span.makeCurrent()) {
            // Get all singer IDs
            Statement statement = Statement.of("SELECT SingerId FROM Singers");
            List<Long> singerIds = new ArrayList<>();
            try (ResultSet resultSet = dbClient.singleUse().executeQuery(statement)) {
                while (resultSet.next()) {
                    singerIds.add(resultSet.getLong("SingerId"));
                }
            }

            if (singerIds.isEmpty()) {
                return null; // No singers found
            }

            // Pick a random ID from the list
            long randomSingerId = singerIds.get(random.nextInt(singerIds.size()));
            span.setAttribute("app.random_singer_id", randomSingerId);
            return getSingerById(randomSingerId);
        } finally {
            span.end();
        }
    }
}
