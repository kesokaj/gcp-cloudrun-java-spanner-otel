# Google Cloud Details
export GCP_PROJECT="your-gcp-project-id"
export SPANNER_INSTANCE_ID="your-spanner-instance-id"
export SPANNER_DATABASE_ID="your-spanner-database-id"

# OpenTelemetry Configuration
export OTEL_SERVICE_NAME="run-java-spanner-demo"
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4317" # For gRPC
export OTEL_METRICS_EXPORTER="otlp"
export OTEL_LOGS_EXPORTER="otlp"
# Optional: To see traces in the console as well as sending them
# export OTEL_TRACES_EXPORTER="otlp,console"

Build and Run the Application: Use Maven to compile the application into a single executable JAR.

# Build the project
mvn clean package

# Run the packaged JAR
java -jar target/run-java-spanner-demo-1.0-SNAPSHOT.jar

3. Viewing the Output
In the Application Console: You will see the application's startup logs and messages for each singer it queries.

In the OTEL Collector Console: You will see detailed output for traces, metrics, and logs as they are received from the Java application. This confirms that your application is successfully exporting telemetry. From the collector, you can then configure other exporters to send the data to backends like Jaeger, Prometheus, or Grafana Loki.

4. Deploying to Cloud Run
This section describes how to deploy the application and the OpenTelemetry collector as a sidecar to Google Cloud Run.

a. Set up Google Cloud Environment
First, you need to set up your Google Cloud environment.

# Set your project ID
gcloud config set project huge-pika-e1h9

# Enable the Artifact Registry API
gcloud services enable artifactregistry.googleapis.com

# Create an Artifact Registry repository
gcloud artifacts repositories create apps --repository-format=docker --location=europe-west1

b. Build and Push the Docker Images
Next, build the Docker images for the application and the OpenTelemetry collector and push them to the Artifact Registry.

# Build the application image
docker build -t run-java-spanner-demo:latest .

# Build the collector image
docker build -t otel-collector ./otelcollector

# Tag the images to match the repository name
docker tag run-java-spanner-demo:latest europe-west1-docker.pkg.dev/huge-pika-e1h9/apps/run-java-spanner-demo:latest
docker tag otel-collector europe-west1-docker.pkg.dev/huge-pika-e1h9/apps/otel-collector

# Push the images to Artifact Registry
docker push europe-west1-docker.pkg.dev/huge-pika-e1h9/apps/run-java-spanner-demo:latest
docker push europe-west1-docker.pkg.dev/huge-pika-e1h9/apps/otel-collector

c. Deploy to Cloud Run
Finally, deploy the service to Cloud Run using the provided `cloud-run.yaml` file.

# Deploy the service
gcloud run services replace cloud-run.yaml --region=europe-west1 --allow-unauthenticated

5. Using the Web Endpoints
The application now exposes several web endpoints. You can access them using the service URL provided by Cloud Run after deployment (e.g., `https://run-java-spanner-demo-xxxxxxxxxx-ew.a.run.app`).

Here are the available paths and example `curl` commands:

*   **Health Check:** Returns "OK".
    ```bash
    curl YOUR_CLOUD_RUN_URL/
    ```

*   **Get All Singers:** Returns a JSON array of all singers.
    ```bash
    curl YOUR_CLOUD_RUN_URL/singers
    ```

*   **Get Random Singer:** Returns a JSON object of a random singer.
    ```bash
    curl YOUR_CLOUD_RUN_URL/singers/random
    ```

*   **Get Singer by ID:** Returns a JSON object for a specific singer. Replace `{id}` with an actual singer ID (e.g., `318619242922318`).
    ```bash
    curl YOUR_CLOUD_RUN_URL/singers/{id}
    ```

6. Verifying OpenTelemetry Traces
After making requests to the web endpoints, you can verify the end-to-end tracing in Google Cloud Trace:

*   Go to **Trace > Trace explorer** in the Google Cloud Console.
*   You should see traces for each web request. Each trace will have a parent span for the incoming HTTP request (named after the path, e.g., `/singers/random`) and a child span for the Spanner database query (e.g., `get-random-singer`). This allows you to follow the entire request flow from the web to the database.