package io.github.kawajava.TerrainAwareRouting.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kawajava.TerrainAwareRouting.domain.RoadSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoJsonRoadLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Value("${app.roads.geojson-path:classpath:roads.geojson}")
    private String geoJsonPath;

    public List<RoadSegment> loadRoadSegments() {
        log.info("Loading road GeoJSON from: {}", geoJsonPath);

        JsonNode root = readGeoJson()
                .orElseThrow(() -> new IllegalArgumentException("Road GeoJSON not found at: " + geoJsonPath));

        JsonNode features = getFeatures(root)
                .orElseThrow(() -> new IllegalArgumentException("Invalid road GeoJSON: missing 'features'"));

        List<RoadSegment> segments = getRoadSegments(features);
        log.info("Loaded {} road segments", segments.size());

        return segments;
    }

    Optional<JsonNode> readGeoJson() {
        try (InputStream is = GeoJsonRoadLoader.class.getClassLoader().getResourceAsStream(geoJsonPath)) {
            return Optional.ofNullable(objectMapper.readTree(is));
        } catch (Exception e) {
            log.error("Failed to read GeoJSON from {}", geoJsonPath, e);
            throw new IllegalStateException("Unable to load road GeoJSON");
        }
    }

    Optional<JsonNode> getFeatures(JsonNode root) {
        JsonNode features = root.get("features");
        return (features != null && features.isArray()) ? Optional.of(features) : Optional.empty();
    }

    List<RoadSegment> getRoadSegments(JsonNode features) {
        return StreamSupport.stream(features.spliterator(), false)
                .map(this::getGeometry)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(this::isLineString)
                .map(this::parseLineString)
                .map(this::createRoadSegment)
                .collect(Collectors.toList());
    }

    Optional<JsonNode> getGeometry(JsonNode feature) {
        return Optional.ofNullable(feature.get("geometry"));
    }

    boolean isLineString(JsonNode geom) {
        return "LineString".equals(geom.get("type").asText());
    }

    LineString parseLineString(JsonNode geom) {
        JsonNode coords = geom.get("coordinates");
        if (coords == null || !coords.isArray()) {
            throw new IllegalArgumentException("LineString missing coordinates");
        }

        Coordinate[] coordinateArray = StreamSupport.stream(coords.spliterator(), false)
                .map(c -> new Coordinate(c.get(0).asDouble(), c.get(1).asDouble()))
                .toArray(Coordinate[]::new);

        return geometryFactory.createLineString(coordinateArray);
    }

    RoadSegment createRoadSegment(LineString lineString) {
        return new RoadSegment(UUID.randomUUID().toString(), lineString, lineString.getLength(), false);
    }
}
