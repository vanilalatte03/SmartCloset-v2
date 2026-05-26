package com.smartcloset.location.presentation;

import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.location.application.LocationService;
import com.smartcloset.location.dto.LocationOptionResponse;
import com.smartcloset.location.dto.LocationResolveRequest;
import com.smartcloset.location.dto.LocationResolveResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/resolve")
    public ApiResponse<LocationResolveResponse> resolveLocation(
            @Valid @RequestBody LocationResolveRequest request
    ) {
        return ApiResponse.of(locationService.resolveLocation(request));
    }
}
