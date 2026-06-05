package com.smartcloset.weather.infrastructure.kma;

import java.util.List;

/**
 * KMA getVilageFcst 원천 데이터를 forecast item 목록으로 가져오는 client boundary다.
 *
 * <p>HTTP transport와 parsing은 구현체가 맡고, 날씨 의미 변환은 mapper/provider 계층에서 처리한다.</p>
 */
public interface KmaForecastClient {

    /**
     * KMA base date/time과 격자 좌표로 getVilageFcst 원천 forecast item을 조회한다.
     */
    List<KmaForecastItem> getVilageForecast(KmaForecastBaseTime baseTime, KmaGrid grid);
}
