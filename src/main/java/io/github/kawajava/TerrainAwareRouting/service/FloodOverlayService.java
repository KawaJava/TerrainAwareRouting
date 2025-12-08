package io.github.kawajava.TerrainAwareRouting.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kawajava.TerrainAwareRouting.domain.RoadSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class FloodOverlayService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Value("${app.flood.backend-url}")
    private String floodBackendUrl;

    private List<Polygon> floodZones;


    public void loadFloodZones() {
        log.info("Downloading flood zones from backend: {}", floodBackendUrl);

        try {
            String json = restTemplate.getForObject(floodBackendUrl, String.class);

            if (json == null || json.isBlank()) {
                throw new IllegalArgumentException("Empty flood response from backend");
            }

            JsonNode root = objectMapper.readTree(json);
            JsonNode features = root.get("features");

            if (features == null || !features.isArray()) {
                throw new IllegalArgumentException("Invalid flood JSON structure – missing 'features'");
            }

            floodZones = StreamSupport.stream(features.spliterator(), false)
                    .map(f -> parseFloodPolygon(f.get("geometry")))
                    .collect(Collectors.toList());

            if (floodZones.isEmpty()) {
                throw new IllegalArgumentException("Flood backend returned zero polygons");
            }

            log.info("Loaded {} flood polygons", floodZones.size());

        } catch (Exception e) {
            log.error("Failed to load flood zones", e);
            throw new IllegalArgumentException("Cannot load flood zones from backend");
        }
    }

    private Polygon parseFloodPolygon(JsonNode geomNode) {

        if (geomNode == null || !"Polygon".equals(geomNode.get("type").asText())) {
            throw new IllegalArgumentException("Invalid flood geometry type – expected Polygon");
        }

        JsonNode coords = geomNode.get("coordinates");
        if (coords == null || !coords.isArray() || coords.isEmpty()) {
            throw new IllegalArgumentException("Polygon missing coordinates");
        }

        JsonNode exteriorRing = coords.get(0);

        Coordinate[] jtsCoords = StreamSupport.stream(exteriorRing.spliterator(), false)
                .map(node -> new Coordinate(node.get(0).asDouble(), node.get(1).asDouble()))
                .toArray(Coordinate[]::new);

        LinearRing shell = geometryFactory.createLinearRing(jtsCoords);
        return geometryFactory.createPolygon(shell);
    }

    public List<RoadSegment> filterSafe(List<RoadSegment> segments) {

        if (floodZones == null) {
            log.warn("Flood polygon cache is empty — loading...");
            loadFloodZones();
        }

        if (segments == null || segments.isEmpty()) {
            throw new NoSuchElementException("No road segments to filter");
        }

        return segments.stream()
                .filter(seg -> isSafe(seg.geometry()))
                .collect(Collectors.toList());
    }

    private boolean isSafe(Geometry road) {
        return floodZones.stream().noneMatch(road::intersects);
    }
}