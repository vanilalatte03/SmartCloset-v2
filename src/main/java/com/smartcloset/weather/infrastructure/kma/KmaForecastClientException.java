package com.smartcloset.weather.infrastructure.kma;

public class KmaForecastClientException extends RuntimeException {

    public KmaForecastClientException(String message) {
        super(message);
    }

    public KmaForecastClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
