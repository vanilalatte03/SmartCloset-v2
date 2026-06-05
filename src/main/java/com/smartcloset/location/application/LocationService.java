package com.smartcloset.location.application;

import com.smartcloset.location.domain.KmaGridConverter;
import com.smartcloset.location.domain.LocationCatalog;
import com.smartcloset.location.domain.LocationGrid;
import com.smartcloset.location.dto.LocationOptionResponse;
import com.smartcloset.location.dto.LocationResolveRequest;
import com.smartcloset.location.dto.LocationResolveResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 위치 검색과 브라우저 좌표 resolve use case를 제공한다.
 *
 * <p>resolve는 가까운 행정구역 후보만 반환하고, 실제 저장은 사용자가 후보를 선택한 뒤
 * UserLocationService에서 수행한다.</p>
 */
@Service
public class LocationService {

    private static final int RESOLVE_CANDIDATE_LIMIT = 5;

    private final LocationCatalog locationCatalog;
    private final KmaGridConverter kmaGridConverter;

    public LocationService(LocationCatalog locationCatalog, KmaGridConverter kmaGridConverter) {
        this.locationCatalog = locationCatalog;
        this.kmaGridConverter = kmaGridConverter;
    }

    /**
     * 내장 KMA 행정구역 catalog를 검색해 저장 가능한 위치 후보를 반환한다.
     */
    public List<LocationOptionResponse> searchLocations(String keyword) {
        return locationCatalog.search(keyword)
                .stream()
                .map(LocationOptionResponse::from)
                .toList();
    }

    /**
     * 브라우저 좌표를 KMA grid로 변환하고 가까운 catalog 후보를 제한 개수만큼 반환한다.
     */
    public LocationResolveResponse resolveLocation(LocationResolveRequest request) {
        LocationGrid grid = kmaGridConverter.toGrid(
                request.latitude().doubleValue(),
                request.longitude().doubleValue()
        );
        return LocationResolveResponse.of(
                grid,
                locationCatalog.findNearest(grid, request.latitude(), request.longitude(), RESOLVE_CANDIDATE_LIMIT)
        );
    }
}
