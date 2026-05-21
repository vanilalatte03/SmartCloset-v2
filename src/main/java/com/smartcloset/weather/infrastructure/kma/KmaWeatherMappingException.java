package com.smartcloset.weather.infrastructure.kma;

public class KmaWeatherMappingException extends RuntimeException {

    public KmaWeatherMappingException(String message) {
        super(message);
    }

    public KmaWeatherMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
