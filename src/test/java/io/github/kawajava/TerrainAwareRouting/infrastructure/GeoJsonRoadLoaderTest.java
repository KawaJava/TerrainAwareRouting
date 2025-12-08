package io.github.kawajava.TerrainAwareRouting.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeoJsonRoadLoaderTest {

    private GeoJsonRoadLoader loader;
    private ObjectMapper objectMapper;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        loader = new GeoJsonRoadLoader();
        objectMapper = new ObjectMapper();
        geometryFactory = new GeometryFactory();
    }

    @Test
    void shouldReadGeoJsonCorrectly() throws Exception {
        String json = """
            {
              "type": "FeatureCollection",
              "features": []
            }
        """;

        JsonNode node = objectMapper.readTree(json);
        assertThat(node).isNotNull();
    }

    @Test
    void shouldGetFeaturesCorrectly() throws Exception {
        String json = """
            {
              "type": "FeatureCollection",
              "features": [ { "geometry": { "type": "LineString", "coordinates": [[0,0],[1,1]] } } ]
            }
        """;

        JsonNode root = objectMapper.readTree(json);
        var features = loader.getFeatures(root);
        assertThat(features).isPresent();
        assertThat(features.get()).hasSize(1);
    }

    @Test
    void shouldReturnEmptyOptionalWhenNoFeatures() throws Exception {
        String json = """
            {
              "type": "FeatureCollection"
            }
        """;

        JsonNode root = objectMapper.readTree(json);
        var features = loader.getFeatures(root);
        assertThat(features).isEmpty();
    }

    @Test
    void shouldIdentifyLineStringCorrectly() throws Exception {
        String json = """
            { "type": "LineString", "coordinates": [[0,0],[1,1]] }
        """;
        JsonNode geom = objectMapper.readTree(json);
        assertThat(loader.isLineString(geom)).isTrue();
    }

    @Test
    void shouldParseLineStringCorrectly() throws Exception {
        String json = """
            { "type": "LineString", "coordinates": [[0,0],[1,1]] }
        """;
        JsonNode geom = objectMapper.readTree(json);

        LineString line = loader.parseLineString(geom);

        assertThat(line).isNotNull();
        assertThat(line.getCoordinates()).hasSize(2);
        assertThat(line.getCoordinateN(0).x).isEqualTo(0.0);
        assertThat(line.getCoordinateN(0).y).isEqualTo(0.0);
        assertThat(line.getCoordinateN(1).x).isEqualTo(1.0);
        assertThat(line.getCoordinateN(1).y).isEqualTo(1.0);
    }

    @Test
    void shouldThrowExceptionForInvalidLineString() {
        String json = """
            { "type": "LineString" }
        """;
        assertThrows(IllegalArgumentException.class, () -> {
            JsonNode geom = objectMapper.readTree(json);
            loader.parseLineString(geom);
        });
    }

    @Test
    void shouldCreateRoadSegmentCorrectly() {
        Coordinate[] coords = new Coordinate[] { new Coordinate(0,0), new Coordinate(1,1) };
        LineString line = geometryFactory.createLineString(coords);

        var segment = loader.createRoadSegment(line);

        assertThat(segment).isNotNull();
        assertThat(segment.geometry()).isEqualTo(line);
        assertThat(segment.cost()).isEqualTo(line.getLength());
        assertThat(segment.id()).isNotBlank();
        assertThat(segment.flooded()).isFalse();
    }

}
