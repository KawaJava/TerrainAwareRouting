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

import java.util.List;
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

        try {
            log.info("Loading road GeoJSON from: {}", geoJsonPath);

            JsonNode root = objectMapper.readTree(
                    GeoJsonRoadLoader.class.getClassLoader().getResourceAsStream(geoJsonPath)
            );

            if (root == null) {
                throw new IllegalArgumentException("Road GeoJSON not found at: " + geoJsonPath);
            }

            JsonNode features = root.get("features");
            if (features == null || !features.isArray()) {
                throw new IllegalArgumentException("Invalid road GeoJSON: missing 'features'");
            }

            List<RoadSegment> segments = getRoadSegments(features);

            log.info("Loaded {} road segments", segments.size());

            return segments;

        } catch (Exception e) {
            log.error("Failed to load GeoJSON roads", e);
            throw new IllegalStateException("Unable to load road GeoJSON");
        }
    }

    private List<RoadSegment> getRoadSegments(JsonNode features) {
        return StreamSupport.stream(features.spliterator(), false)
                .map(f -> f.get("geometry"))
                .filter(geom -> geom != null)
                .filter(geom -> "LineString".equals(geom.get("type").asText()))
                .map(this::parseLineString)
                .map(ls -> new RoadSegment(UUID.randomUUID().toString(), ls, ls.getLength(), false))
                .collect(Collectors.toList());
    }

    private LineString parseLineString(JsonNode geom) {

        JsonNode coords = geom.get("coordinates");
        if (coords == null || !coords.isArray()) {
            throw new IllegalArgumentException("LineString missing coordinates");
        }

        Coordinate[] coordArr = StreamSupport.stream(coords.spliterator(), false)
                .map(c -> new Coordinate(c.get(0).asDouble(), c.get(1).asDouble()))
                .toArray(Coordinate[]::new);

        return geometryFactory.createLineString(coordArr);
    }
}
