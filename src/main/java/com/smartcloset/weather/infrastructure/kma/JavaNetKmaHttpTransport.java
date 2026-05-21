package com.smartcloset.weather.infrastructure.kma;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class JavaNetKmaHttpTransport implements KmaHttpTransport {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;

    JavaNetKmaHttpTransport() {
        this(HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build());
    }

    JavaNetKmaHttpTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public KmaHttpResponse get(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new KmaHttpResponse(response.statusCode(), response.body());
    }
}
