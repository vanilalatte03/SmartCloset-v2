package com.smartcloset.location.controller;

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

/**
 * 내장 KMA 위치 catalog 검색과 브라우저 좌표 resolve API를 제공한다.
 *
 * <p>외부 지도/주소 API를 호출하지 않고, 현재 위치 후보는 저장용 좌표가 아니라 catalog 선택지로만 반환한다.</p>
 */
@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * 내장 위치 catalog에서 keyword와 일치하는 후보를 검색한다.
     */
    @GetMapping
    public ApiResponse<List<LocationOptionResponse>> getLocations(
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.of(locationService.searchLocations(keyword));
    }

    /**
     * 브라우저 좌표를 KMA grid로 변환하고 가까운 저장 가능 위치 후보를 반환한다.
     */
    @PostMapping("/resolve")
    public ApiResponse<LocationResolveResponse> resolveLocation(
            @Valid @RequestBody LocationResolveRequest request
    ) {
        return ApiResponse.of(locationService.resolveLocation(request));
    }
}
