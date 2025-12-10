package io.github.kawajava.TerrainAwareRouting.controller;

import io.github.kawajava.TerrainAwareRouting.controller.dto.RouteResponse;
import io.github.kawajava.TerrainAwareRouting.controller.dto.RouteStep;
import io.github.kawajava.TerrainAwareRouting.domain.RoadSegment;
import io.github.kawajava.TerrainAwareRouting.infrastructure.GeoJsonRoadLoader;
import io.github.kawajava.TerrainAwareRouting.service.FloodOverlayService;
import io.github.kawajava.TerrainAwareRouting.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RouteController {

    private final GeoJsonRoadLoader loader;
    private final FloodOverlayService floodService;
    private final RouteService routing;

    @GetMapping("/api/evac/route")
    public ResponseEntity<RouteResponse> route(@RequestParam String start, @RequestParam String end) {
        Coordinate startCoord = parseCoord(start);
        Coordinate endCoord = parseCoord(end);

        List<RoadSegment> roads = loader.loadRoadSegments();
        List<RoadSegment> withFlood = floodService.filterSafe(roads);

        List<Coordinate> coords = routing.computeRoute(withFlood, startCoord, endCoord);

        List<RouteStep> steps = coords.stream()
                .map(c -> new RouteStep(c.y, c.x))
                .toList();

        return ResponseEntity.ok(new RouteResponse(steps, 0));
    }

    private Coordinate parseCoord(String raw) {
        String[] parts = raw.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Coordinates must be in format lat,lon");
        }

        double lat = Double.parseDouble(parts[0]);
        double lon = Double.parseDouble(parts[1]);

        return new Coordinate(lon, lat);
    }
}
