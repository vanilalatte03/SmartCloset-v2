package com.smartcloset.weather.infrastructure.kma;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * KMA 날씨 provider와 fallback 동작에 필요한 외부 설정을 바인딩한다.
 *
 * <p>Service key가 없거나 provider가 실패할 때 fallback 사용 여부를 이 설정으로 결정한다.</p>
 */
@ConfigurationProperties(prefix = "smartcloset.weather")
public class KmaWeatherProperties {

    public static final String DEFAULT_BASE_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0";
    public static final int DEFAULT_NX = 60;
    public static final int DEFAULT_NY = 127;
    public static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(2);
    public static final int DEFAULT_CACHE_MAX_SIZE = 256;
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(3);
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(5);
    public static final int DEFAULT_MAX_ATTEMPTS = 2;
    public static final Duration DEFAULT_RETRY_BACKOFF = Duration.ofMillis(200);
    public static final int DEFAULT_CIRCUIT_BREAKER_FAILURE_THRESHOLD = 3;
    public static final Duration DEFAULT_CIRCUIT_BREAKER_OPEN_DURATION = Duration.ofSeconds(30);

    private final Kma kma = new Kma();

    private boolean fallbackEnabled = true;

    public String serviceKey() {
        return kma.serviceKey;
    }

    public int nx() {
        return kma.nx;
    }

    public int ny() {
        return kma.ny;
    }

    public String baseUrl() {
        return kma.baseUrl;
    }

    public Duration cacheTtl() {
        return kma.cacheTtl;
    }

    public int cacheMaxSize() {
        return kma.cacheMaxSize;
    }

    public Duration connectTimeout() {
        return kma.connectTimeout;
    }

    public Duration readTimeout() {
        return kma.readTimeout;
    }

    public Duration requestTimeout() {
        return kma.requestTimeout;
    }

    public int maxAttempts() {
        return kma.maxAttempts;
    }

    public Duration retryBackoff() {
        return kma.retryBackoff;
    }

    public int circuitBreakerFailureThreshold() {
        return kma.circuitBreakerFailureThreshold;
    }

    public Duration circuitBreakerOpenDuration() {
        return kma.circuitBreakerOpenDuration;
    }

    public boolean fallbackEnabled() {
        return fallbackEnabled;
    }

    public Kma getKma() {
        return kma;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public static class Kma {

        private String serviceKey = "";

        private int nx = DEFAULT_NX;

        private int ny = DEFAULT_NY;

        private String baseUrl = DEFAULT_BASE_URL;

        private Duration cacheTtl = DEFAULT_CACHE_TTL;

        private int cacheMaxSize = DEFAULT_CACHE_MAX_SIZE;

        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;

        private Duration readTimeout = DEFAULT_READ_TIMEOUT;

        private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;

        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

        private Duration retryBackoff = DEFAULT_RETRY_BACKOFF;

        private int circuitBreakerFailureThreshold = DEFAULT_CIRCUIT_BREAKER_FAILURE_THRESHOLD;

        private Duration circuitBreakerOpenDuration = DEFAULT_CIRCUIT_BREAKER_OPEN_DURATION;

        public String getServiceKey() {
            return serviceKey;
        }

        public void setServiceKey(String serviceKey) {
            this.serviceKey = serviceKey == null ? "" : serviceKey;
        }

        public int getNx() {
            return nx;
        }

        public void setNx(int nx) {
            this.nx = nx;
        }

        public int getNy() {
            return ny;
        }

        public void setNy(int ny) {
            this.ny = ny;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(Duration cacheTtl) {
            this.cacheTtl = cacheTtl == null ? DEFAULT_CACHE_TTL : cacheTtl;
        }

        public int getCacheMaxSize() {
            return cacheMaxSize;
        }

        public void setCacheMaxSize(int cacheMaxSize) {
            this.cacheMaxSize = cacheMaxSize;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout == null ? DEFAULT_READ_TIMEOUT : readTimeout;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout == null ? DEFAULT_REQUEST_TIMEOUT : requestTimeout;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getRetryBackoff() {
            return retryBackoff;
        }

        public void setRetryBackoff(Duration retryBackoff) {
            this.retryBackoff = retryBackoff == null ? DEFAULT_RETRY_BACKOFF : retryBackoff;
        }

        public int getCircuitBreakerFailureThreshold() {
            return circuitBreakerFailureThreshold;
        }

        public void setCircuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) {
            this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
        }

        public Duration getCircuitBreakerOpenDuration() {
            return circuitBreakerOpenDuration;
        }

        public void setCircuitBreakerOpenDuration(Duration circuitBreakerOpenDuration) {
            this.circuitBreakerOpenDuration = circuitBreakerOpenDuration == null
                    ? DEFAULT_CIRCUIT_BREAKER_OPEN_DURATION
                    : circuitBreakerOpenDuration;
        }
    }
}
