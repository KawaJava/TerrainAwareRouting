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
import java.util.Optional;
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
    String floodBackendUrl;
    List<Polygon> floodZones;

    public void loadFloodZones() {
        log.info("Downloading flood zones from backend: {}", floodBackendUrl);

        String json = fetchFloodData();
        List<JsonNode> features = extractFeatures(json);
        floodZones = parsePolygons(features);

        if (floodZones.isEmpty()) {
            throw new IllegalArgumentException("Flood backend returned zero polygons");
        }

        log.info("Loaded {} flood polygons", floodZones.size());
    }

    public String fetchFloodData() {
        try {
            String json = restTemplate.getForObject(floodBackendUrl, String.class);
            if (Optional.ofNullable(json).filter(s -> !s.isBlank()).isEmpty()) {
                throw new IllegalArgumentException("Empty flood response from backend");
            }
            return json;
        } catch (Exception e) {
            log.error("Failed to fetch flood data", e);
            throw new IllegalArgumentException("Cannot fetch flood zones from backend");
        }
    }

    public List<JsonNode> extractFeatures(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode features = root.get("features");
            if (features == null || !features.isArray()) {
                throw new IllegalArgumentException("Invalid flood JSON structure – missing 'features'");
            }
            return StreamSupport.stream(features.spliterator(), false)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse flood JSON", e);
            throw new IllegalArgumentException("Cannot parse flood JSON");
        }
    }

    public List<Polygon> parsePolygons(List<JsonNode> features) {
        return features.stream()
                .map(f -> parsePolygon(f.get("geometry")))
                .collect(Collectors.toList());
    }

    public Polygon parsePolygon(JsonNode geomNode) {
        validateGeometryNode(geomNode);
        Coordinate[] coords = extractCoordinates(geomNode);
        LinearRing shell = geometryFactory.createLinearRing(coords);
        return geometryFactory.createPolygon(shell);
    }

    public void validateGeometryNode(JsonNode geomNode) {
        if (geomNode == null || !"Polygon".equals(geomNode.get("type").asText())) {
            throw new IllegalArgumentException("Invalid flood geometry type – expected Polygon");
        }
        JsonNode coords = geomNode.get("coordinates");
        if (coords == null || !coords.isArray() || coords.isEmpty()) {
            throw new IllegalArgumentException("Polygon missing coordinates");
        }
    }

    public Coordinate[] extractCoordinates(JsonNode geomNode) {
        JsonNode exteriorRing = geomNode.get("coordinates").get(0);
        return StreamSupport.stream(exteriorRing.spliterator(), false)
                .map(node -> new Coordinate(node.get(0).asDouble(), node.get(1).asDouble()))
                .toArray(Coordinate[]::new);
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

    public boolean isSafe(Geometry road) {
        return floodZones.stream().noneMatch(road::intersects);
    }
}
