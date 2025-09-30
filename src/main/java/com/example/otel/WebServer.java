package com.example.otel;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class WebServer {

    private final SpannerService spannerService;
    private final Tracer tracer;

    public WebServer(SpannerService spannerService, OpenTelemetry openTelemetry) {
        this.spannerService = spannerService;
        this.tracer = openTelemetry.getTracer(WebServer.class.getName(), "1.0.0");
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new MyHandler(spannerService, tracer));
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {
        private final SpannerService spannerService;
        private final Tracer tracer;

        public MyHandler(SpannerService spannerService, Tracer tracer) {
            this.spannerService = spannerService;
            this.tracer = tracer;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            Span span = tracer.spanBuilder(t.getRequestURI().getPath()).startSpan();
            try (Scope scope = span.makeCurrent()) {
                span.setAttribute("http.method", t.getRequestMethod());
                span.setAttribute("http.url", t.getRequestURI().toString());

                String path = t.getRequestURI().getPath();
                String response = "";
                int statusCode = 200;

                if (path.equals("/")) {
                    response = "OK";
                } else if (path.equals("/singers")) {
                    response = spannerService.getAllSingers();
                    t.getResponseHeaders().set("Content-Type", "application/json");
                } else if (path.equals("/singers/random")) {
                    String singerJson = spannerService.getRandomSinger();
                    if (singerJson != null) {
                        response = singerJson;
                        t.getResponseHeaders().set("Content-Type", "application/json");
                    } else {
                        response = "Singer not found";
                        statusCode = 404;
                    }
                } else if (path.startsWith("/singers/")) {
                    try {
                        String idStr = path.substring("/singers/".length());
                        long singerId = Long.parseLong(idStr);
                        String singerJson = spannerService.getSingerById(singerId);
                        if (singerJson != null) {
                            response = singerJson;
                            t.getResponseHeaders().set("Content-Type", "application/json");
                        } else {
                            response = "Singer not found";
                            statusCode = 404;
                        }
                    } catch (NumberFormatException e) {
                        response = "Invalid singer ID";
                        statusCode = 400;
                    }
                } else {
                    response = "Not found";
                    statusCode = 404;
                }

                span.setAttribute("http.status_code", statusCode);
                t.sendResponseHeaders(statusCode, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } finally {
                span.end();
            }
        }
    }
}