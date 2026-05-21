package com.smartcloset.weather.infrastructure.kma;

import java.util.List;

public interface KmaForecastClient {

    List<KmaForecastItem> getVilageForecast(KmaForecastBaseTime baseTime);
}
