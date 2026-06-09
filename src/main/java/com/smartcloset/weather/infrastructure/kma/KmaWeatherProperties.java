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
    }
}
