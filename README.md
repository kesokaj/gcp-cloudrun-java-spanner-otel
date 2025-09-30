Java, OpenTelemetry, and Spanner Demo
This is a simple Java application that demonstrates how to instrument an application that connects to Google Cloud Spanner using the OpenTelemetry SDK. It generates traces, metrics, and logs and exports them via OTLP (OpenTelemetry Protocol).

1. Prerequisites
a. Google Cloud Spanner Setup
Create a Spanner Instance and Database: If you don't have one, create a Spanner instance and a database using the Google Cloud Console or gcloud CLI.

Create the Table: Connect to your database and run the following DDL statement to create the required table:

CREATE TABLE Singers (
  SingerId   INT64 NOT NULL,
  FirstName  STRING(1024),
  LastName   STRING(1024),
) PRIMARY KEY (SingerId);

Insert Sample Data: Run the following DML to insert some data to query:

INSERT INTO Singers (SingerId, FirstName, LastName)
VALUES (1, 'Marc', 'Richards'),
       (2, 'Catalina', 'Smith'),
       (3, 'Alice', 'Trentor'),
       (4, 'Lea', 'Martin'),
       (5, 'David', 'Lomond');

b. Google Cloud Authentication
The application uses Application Default Credentials (ADC) to authenticate with Spanner. The easiest way to set this up for local development is to use the gcloud CLI:

gcloud auth application-default login

c. OpenTelemetry Collector
To receive the telemetry data from this application, you need an OpenTelemetry Collector.

Download the appropriate binary from the OpenTelemetry Collector releases page.

Create a configuration file named otel-collector-config.yaml with the following content. This configuration receives data via OTLP and exports everything to the console (logging exporter) for easy inspection.

receivers:
  otlp:
    protocols:
      grpc:
      http:

processors:
  batch:

exporters:
  logging:
    loglevel: debug

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging]
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging]

Run the collector from your terminal:

./otelcol-contrib --config ./otel-collector-config.yaml

2. Running the Application
Set Environment Variables: Before running the app, you must configure it with your Spanner and project details, and tell it where to send telemetry data.

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
docker build -t run-java-spanner-demo .

# Build the collector image
docker build -t otel-collector ./otelcollector

# Tag the images to match the repository name
docker tag run-java-spanner-demo europe-west1-docker.pkg.dev/huge-pika-e1h9/apps/run-java-spanner-demo
docker tag otel-collector europe-west1-docker.pkg.dev/huge-pika-e1h9/apps/otel-collector

# Push the images to Artifact Registry
docker push europe-west1-docker.pkg.dev/huge-pika-e1h9/apps/run-java-spanner-demo
docker push europe-west1-docker.pkg.dev/huge-pika-e1h9/apps/otel-collector

c. Deploy to Cloud Run
Finally, deploy the service to Cloud Run using the provided `cloud-run.yaml` file.

# Deploy the service
gcloud run services replace cloud-run.yaml --region=europe-west1 --allow-unauthenticated