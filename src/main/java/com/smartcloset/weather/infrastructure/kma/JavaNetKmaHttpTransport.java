package com.smartcloset.weather.infrastructure.kma;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * JDK {@link HttpURLConnection}으로 KMA getVilageFcst endpoint를 호출하는 기본 transport 구현이다.
 */
final class JavaNetKmaHttpTransport implements KmaHttpTransport, AutoCloseable {

    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final Duration requestTimeout;
    private final ExecutorService executorService;

    JavaNetKmaHttpTransport(KmaWeatherProperties properties) {
        this(
                properties.connectTimeout(),
                properties.readTimeout(),
                properties.requestTimeout(),
                Executors.newVirtualThreadPerTaskExecutor()
        );
    }

    JavaNetKmaHttpTransport(
            Duration connectTimeout,
            Duration readTimeout,
            Duration requestTimeout,
            ExecutorService executorService
    ) {
        this.connectTimeout = positive(connectTimeout, "connectTimeout");
        this.readTimeout = positive(readTimeout, "readTimeout");
        this.requestTimeout = positive(requestTimeout, "requestTimeout");
        this.executorService = Objects.requireNonNull(executorService, "executorService must not be null");
    }

    @Override
    public KmaHttpResponse get(URI uri) throws IOException, InterruptedException {
        Future<KmaHttpResponse> responseFuture = executorService.submit(() -> blockingGet(uri));
        try {
            return responseFuture.get(timeoutMillis(requestTimeout), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            responseFuture.cancel(true);
            throw new IOException("KMA forecast request timed out", exception);
        } catch (InterruptedException exception) {
            responseFuture.cancel(true);
            Thread.currentThread().interrupt();
            throw exception;
        } catch (ExecutionException exception) {
            throw unwrapExecutionException(exception);
        }
    }

    /**
     * 전체 request timeout을 감싸는 executor를 종료한다.
     */
    @Override
    public void close() {
        executorService.shutdownNow();
    }

    /**
     * 연결과 body 읽기 timeout을 HttpURLConnection에 직접 적용해 GET 요청을 수행한다.
     */
    private KmaHttpResponse blockingGet(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMillis(connectTimeout));
            connection.setReadTimeout(timeoutMillis(readTimeout));
            connection.setRequestProperty("Accept", "application/json");

            int statusCode = connection.getResponseCode();
            return new KmaHttpResponse(statusCode, readBody(connection, statusCode));
        } finally {
            connection.disconnect();
        }
    }

    private String readBody(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private IOException unwrapExecutionException(ExecutionException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof IOException ioException) {
            return ioException;
        }
        if (cause instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        return new IOException("KMA forecast request failed", cause);
    }

    private Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("KMA " + name + " must be positive");
        }
        return value;
    }

    private int timeoutMillis(Duration timeout) {
        long millis = timeout.toMillis();
        if (millis > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) millis);
    }
}
