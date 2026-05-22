package com.smartcloset.location.application;

import com.smartcloset.location.domain.LocationCatalog;
import com.smartcloset.location.dto.LocationOptionResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    private final LocationCatalog locationCatalog;

    public LocationService(LocationCatalog locationCatalog) {
        this.locationCatalog = locationCatalog;
    }

    public List<LocationOptionResponse> searchLocations(String keyword) {
        return locationCatalog.search(keyword)
                .stream()
                .map(LocationOptionResponse::from)
                .toList();
    }
}
