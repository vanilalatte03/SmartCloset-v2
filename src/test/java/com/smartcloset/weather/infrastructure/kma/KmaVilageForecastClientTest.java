package com.smartcloset.weather.infrastructure.kma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

class KmaVilageForecastClientTest {

    private final KmaForecastBaseTime baseTime = new KmaForecastBaseTime("20260521", "1400");
    private final KmaGrid grid = new KmaGrid(98, 76);

    @Test
    void requestsDocumentedGetVilageFcstParametersAndReturnsItems() {
        FakeTransport transport = FakeTransport.responding(200, successResponse());
        KmaVilageForecastClient client = newClient(transport);

        List<KmaForecastItem> items = client.getVilageForecast(baseTime, grid);

        assertThat(transport.requestedUri().getPath()).isEqualTo("/kma/getVilageFcst");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(transport.requestedUri())
                .build()
                .getQueryParams();
        assertThat(queryParams.getFirst("serviceKey")).isEqualTo("test-service-key");
        assertThat(queryParams.getFirst("pageNo")).isEqualTo("1");
        assertThat(queryParams.getFirst("numOfRows")).isEqualTo("1000");
        assertThat(queryParams.getFirst("dataType")).isEqualTo("JSON");
        assertThat(queryParams.getFirst("base_date")).isEqualTo("20260521");
        assertThat(queryParams.getFirst("base_time")).isEqualTo("1400");
        assertThat(queryParams.getFirst("nx")).isEqualTo("98");
        assertThat(queryParams.getFirst("ny")).isEqualTo("76");

        assertThat(items).containsExactly(
                new KmaForecastItem("20260521", "1500", "TMP", "13"),
                new KmaForecastItem("20260521", "1500", "SKY", "3")
        );
    }

    @Test
    void encodesDecodedServiceKeyReservedCharactersForQueryString() {
        FakeTransport transport = FakeTransport.responding(200, successResponse());
        KmaVilageForecastClient client = newClient(transport, "abc+def/ghi==");

        client.getVilageForecast(baseTime, grid);

        assertThat(transport.requestedUri().getRawQuery())
                .contains("serviceKey=abc%2Bdef%2Fghi%3D%3D")
                .doesNotContain("serviceKey=abc+def/ghi==");
    }

    @Test
    void doesNotDoubleEncodeAlreadyEncodedServiceKey() {
        FakeTransport transport = FakeTransport.responding(200, successResponse());
        KmaVilageForecastClient client = newClient(transport, "abc%2Bdef%2Fghi%3D%3D");

        client.getVilageForecast(baseTime, grid);

        assertThat(transport.requestedUri().getRawQuery())
                .contains("serviceKey=abc%2Bdef%2Fghi%3D%3D")
                .doesNotContain("%252B")
                .doesNotContain("%252F")
                .doesNotContain("%253D");
    }

    @Test
    void failsWhenResultCodeIsNotSuccess() {
        KmaVilageForecastClient client = newClient(FakeTransport.responding(200, """
                {
                  "response": {
                    "header": {
                      "resultCode": "99",
                      "resultMsg": "SERVICE_ERROR"
                    },
                    "body": {
                      "items": {
                        "item": []
                      }
                    }
                  }
                }
                """));

        assertThatThrownBy(() -> client.getVilageForecast(baseTime, grid))
                .isInstanceOf(KmaForecastClientException.class)
                .hasMessageContaining("99")
                .hasMessageContaining("SERVICE_ERROR");
    }

    @Test
    void failsWhenKmaReturnsNoDataError() {
        KmaVilageForecastClient client = newClient(FakeTransport.responding(200, """
                {
                  "response": {
                    "header": {
                      "resultCode": "03",
                      "resultMsg": "NODATA_ERROR"
                    }
                  }
                }
                """));

        assertThatThrownBy(() -> client.getVilageForecast(baseTime, grid))
                .isInstanceOf(KmaForecastClientException.class)
                .hasMessageContaining("03")
                .hasMessageContaining("NODATA_ERROR");
    }

    @Test
    void failsWhenItemsAreEmpty() {
        KmaVilageForecastClient client = newClient(FakeTransport.responding(200, """
                {
                  "response": {
                    "header": {
                      "resultCode": "00",
                      "resultMsg": "NORMAL_SERVICE"
                    },
                    "body": {
                      "items": {
                        "item": []
                      }
                    }
                  }
                }
                """));

        assertThatThrownBy(() -> client.getVilageForecast(baseTime, grid))
                .isInstanceOf(KmaForecastClientException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void failsWhenResponseCannotBeParsed() {
        KmaVilageForecastClient client = newClient(FakeTransport.responding(200, "{ invalid json"));

        assertThatThrownBy(() -> client.getVilageForecast(baseTime, grid))
                .isInstanceOf(KmaForecastClientException.class)
                .hasMessageContaining("parse KMA forecast response");
    }

    @Test
    void failsWhenHttpStatusIsNotSuccessful() {
        KmaVilageForecastClient client = newClient(FakeTransport.responding(500, successResponse()));

        assertThatThrownBy(() -> client.getVilageForecast(baseTime, grid))
                .isInstanceOf(KmaForecastClientException.class)
                .hasMessageContaining("HTTP status 500");
    }

    @Test
    void failsWhenTransportThrows() {
        KmaVilageForecastClient client = newClient(FakeTransport.failing(new IOException("socket closed")));

        assertThatThrownBy(() -> client.getVilageForecast(baseTime, grid))
                .isInstanceOf(KmaForecastClientException.class)
                .hasMessageContaining("Failed to call KMA forecast API");
    }

    private KmaVilageForecastClient newClient(FakeTransport transport) {
        return newClient(transport, "test-service-key");
    }

    private KmaVilageForecastClient newClient(FakeTransport transport, String serviceKey) {
        KmaWeatherProperties properties = new KmaWeatherProperties();
        properties.getKma().setServiceKey(serviceKey);
        properties.getKma().setBaseUrl("http://example.test/kma");
        properties.getKma().setNx(61);
        properties.getKma().setNy(128);
        return new KmaVilageForecastClient(properties, new ObjectMapper(), transport);
    }

    private static String successResponse() {
        return """
                {
                  "response": {
                    "header": {
                      "resultCode": "00",
                      "resultMsg": "NORMAL_SERVICE"
                    },
                    "body": {
                      "items": {
                        "item": [
                          {
                            "fcstDate": "20260521",
                            "fcstTime": "1500",
                            "category": "TMP",
                            "fcstValue": "13"
                          },
                          {
                            "fcstDate": "20260521",
                            "fcstTime": "1500",
                            "category": "SKY",
                            "fcstValue": "3"
                          }
                        ]
                      }
                    }
                  }
                }
                """;
    }

    private static final class FakeTransport implements KmaHttpTransport {

        private final KmaHttpResponse response;
        private final IOException failure;
        private URI requestedUri;

        private FakeTransport(KmaHttpResponse response, IOException failure) {
            this.response = response;
            this.failure = failure;
        }

        static FakeTransport responding(int statusCode, String body) {
            return new FakeTransport(new KmaHttpResponse(statusCode, body), null);
        }

        static FakeTransport failing(IOException failure) {
            return new FakeTransport(null, failure);
        }

        @Override
        public KmaHttpResponse get(URI uri) throws IOException {
            this.requestedUri = uri;
            if (failure != null) {
                throw failure;
            }
            return response;
        }

        URI requestedUri() {
            return requestedUri;
        }
    }
}
