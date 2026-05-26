package com.smartcloset.location.application;

import com.smartcloset.location.domain.KmaGridConverter;
import com.smartcloset.location.domain.LocationCatalog;
import com.smartcloset.location.domain.LocationGrid;
import com.smartcloset.location.dto.LocationOptionResponse;
import com.smartcloset.location.dto.LocationResolveRequest;
import com.smartcloset.location.dto.LocationResolveResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    private static final int RESOLVE_CANDIDATE_LIMIT = 5;

    private final LocationCatalog locationCatalog;
    private final KmaGridConverter kmaGridConverter;

    public LocationService(LocationCatalog locationCatalog, KmaGridConverter kmaGridConverter) {
        this.locationCatalog = locationCatalog;
        this.kmaGridConverter = kmaGridConverter;
    }

    public List<LocationOptionResponse> searchLocations(String keyword) {
        return locationCatalog.search(keyword)
                .stream()
                .map(LocationOptionResponse::from)
                .toList();
    }

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
