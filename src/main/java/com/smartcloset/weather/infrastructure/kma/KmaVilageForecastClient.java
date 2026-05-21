package com.smartcloset.weather.infrastructure.kma;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KmaVilageForecastClient implements KmaForecastClient {

    private static final String GET_VILAGE_FCST_PATH = "getVilageFcst";
    private static final String SUCCESS_RESULT_CODE = "00";

    private final KmaWeatherProperties properties;
    private final ObjectMapper objectMapper;
    private final KmaHttpTransport transport;

    @Autowired
    public KmaVilageForecastClient(KmaWeatherProperties properties) {
        this(properties, new ObjectMapper(), new JavaNetKmaHttpTransport());
    }

    KmaVilageForecastClient(
            KmaWeatherProperties properties,
            ObjectMapper objectMapper,
            KmaHttpTransport transport
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
    }

    @Override
    public List<KmaForecastItem> getVilageForecast(KmaForecastBaseTime baseTime) {
        Objects.requireNonNull(baseTime, "baseTime must not be null");
        URI uri = buildUri(baseTime);
        KmaHttpResponse response = execute(uri);
        validateHttpStatus(response);
        return parseItems(response.body());
    }

    URI buildUri(KmaForecastBaseTime baseTime) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .pathSegment(GET_VILAGE_FCST_PATH)
                .queryParam("serviceKey", properties.serviceKey())
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", "1000")
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseTime.baseDate())
                .queryParam("base_time", baseTime.baseTime())
                .queryParam("nx", properties.nx())
                .queryParam("ny", properties.ny())
                .build()
                .toUri();
    }

    private KmaHttpResponse execute(URI uri) {
        try {
            return transport.get(uri);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KmaForecastClientException("KMA forecast request was interrupted", exception);
        } catch (IOException exception) {
            throw new KmaForecastClientException("Failed to call KMA forecast API", exception);
        }
    }

    private void validateHttpStatus(KmaHttpResponse response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new KmaForecastClientException("KMA forecast API returned HTTP status " + response.statusCode());
        }
    }

    private List<KmaForecastItem> parseItems(String responseBody) {
        JsonNode root = readTree(responseBody);
        JsonNode header = required(root, "response", "response")
                .path("header");
        if (header.isMissingNode() || header.isNull()) {
            throw new KmaForecastClientException("KMA forecast response is missing response.header");
        }

        String resultCode = requiredText(header, "resultCode", "response.header.resultCode");
        String resultMsg = optionalText(header, "resultMsg");
        if (!SUCCESS_RESULT_CODE.equals(resultCode)) {
            throw new KmaForecastClientException(
                    "KMA forecast response resultCode is not success: " + resultCode + " " + resultMsg
            );
        }

        JsonNode itemNode = required(root, "response", "response")
                .path("body")
                .path("items")
                .path("item");
        return parseItemNode(itemNode);
    }

    private JsonNode readTree(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new KmaForecastClientException("Failed to parse KMA forecast response", exception);
        }
    }

    private List<KmaForecastItem> parseItemNode(JsonNode itemNode) {
        if (itemNode.isMissingNode() || itemNode.isNull()) {
            throw new KmaForecastClientException("KMA forecast response items.item is missing");
        }

        List<KmaForecastItem> items = new ArrayList<>();
        if (itemNode.isArray()) {
            for (JsonNode item : itemNode) {
                items.add(toForecastItem(item));
            }
        } else if (itemNode.isObject()) {
            items.add(toForecastItem(itemNode));
        } else {
            throw new KmaForecastClientException("KMA forecast response items.item has invalid shape");
        }

        if (items.isEmpty()) {
            throw new KmaForecastClientException("KMA forecast response items.item is empty");
        }
        return List.copyOf(items);
    }

    private KmaForecastItem toForecastItem(JsonNode item) {
        return new KmaForecastItem(
                requiredText(item, "fcstDate", "items.item.fcstDate"),
                requiredText(item, "fcstTime", "items.item.fcstTime"),
                requiredText(item, "category", "items.item.category"),
                requiredText(item, "fcstValue", "items.item.fcstValue")
        );
    }

    private JsonNode required(JsonNode node, String fieldName, String path) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            throw new KmaForecastClientException("KMA forecast response is missing " + path);
        }
        return value;
    }

    private String requiredText(JsonNode node, String fieldName, String path) {
        String value = optionalText(node, fieldName);
        if (value == null || value.isBlank()) {
            throw new KmaForecastClientException("KMA forecast response is missing " + path);
        }
        return value;
    }

    private String optionalText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
