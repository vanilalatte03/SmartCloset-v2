package com.smartcloset.location.presentation;

import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.location.application.LocationService;
import com.smartcloset.location.dto.LocationOptionResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public ApiResponse<List<LocationOptionResponse>> getLocations(
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.of(locationService.searchLocations(keyword));
    }
}
