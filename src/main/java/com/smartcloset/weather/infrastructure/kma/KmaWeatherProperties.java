package com.smartcloset.weather.infrastructure.kma;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartcloset.weather")
public class KmaWeatherProperties {

    public static final String DEFAULT_BASE_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0";
    public static final int DEFAULT_NX = 60;
    public static final int DEFAULT_NY = 127;

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
    }
}
