package io.github.kawajava.TerrainAwareRouting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kawajava.TerrainAwareRouting.domain.RoadSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class FloodOverlayServiceTest {

    private ObjectMapper objectMapper;
    private GeometryFactory geometryFactory;
    private FloodOverlayService service;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        geometryFactory = new GeometryFactory();
        service = new FloodOverlayService();
        service.floodBackendUrl = "http://mock-url";
    }

    @Test
    public void shouldExtractFeaturesCorrectly() throws Exception {
        String json = """
                {
                  "features": [
                    {"geometry": {"type": "Polygon", "coordinates": [[[0,0],[1,0],[1,1],[0,1],[0,0]]]}}
                  ]
                }
                """;

        List<JsonNode> features = service.extractFeatures(json);

        assertThat(features).hasSize(1);
        assertThat(features.get(0).get("geometry").get("type").asText()).isEqualTo("Polygon");
    }

    @Test
    public void shouldParsePolygonCorrectly() {
        JsonNode geomNode = objectMapper.createObjectNode()
                .put("type", "Polygon")
                .set("coordinates", objectMapper.createArrayNode()
                        .add(objectMapper.createArrayNode()
                                .add(objectMapper.createArrayNode().add(0).add(0))
                                .add(objectMapper.createArrayNode().add(1).add(0))
                                .add(objectMapper.createArrayNode().add(1).add(1))
                                .add(objectMapper.createArrayNode().add(0).add(1))
                                .add(objectMapper.createArrayNode().add(0).add(0))
                        )
                );

        Polygon polygon = service.parsePolygon(geomNode);

        assertThat(polygon).isNotNull();
        assertThat(polygon.getCoordinates()).hasSize(5);
    }

    @Test
    public void shouldFilterSafeSegmentsCorrectly() {
        Polygon floodPolygon = geometryFactory.createPolygon(new Coordinate[]{
                new Coordinate(0,0),
                new Coordinate(2,0),
                new Coordinate(2,2),
                new Coordinate(0,2),
                new Coordinate(0,0)
        });
        service.floodZones = List.of(floodPolygon);

        LineString safeRoad = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(3,3),
                new Coordinate(4,4)
        });
        RoadSegment safeSegment = new RoadSegment("1", safeRoad, 10, false);

        LineString floodedRoad = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(1,1),
                new Coordinate(3,3)
        });
        RoadSegment floodedSegment = new RoadSegment("2", floodedRoad, 20, true);

        List<RoadSegment> segments = Arrays.asList(safeSegment, floodedSegment);

        List<RoadSegment> result = service.filterSafe(segments);

        assertThat(result).containsExactly(safeSegment);
    }

    @Test
    public void shouldReturnTrueForSafeGeometry() {
        Polygon floodPolygon = geometryFactory.createPolygon(new Coordinate[]{
                new Coordinate(0,0),
                new Coordinate(1,0),
                new Coordinate(1,1),
                new Coordinate(0,1),
                new Coordinate(0,0)
        });
        service.floodZones = List.of(floodPolygon);

        LineString road = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(2,2),
                new Coordinate(3,3)
        });

        assertThat(service.isSafe(road)).isTrue();
    }

    @Test
    public void shouldReturnFalseForIntersectingGeometry() {
        Polygon floodPolygon = geometryFactory.createPolygon(new Coordinate[]{
                new Coordinate(0,0),
                new Coordinate(2,0),
                new Coordinate(2,2),
                new Coordinate(0,2),
                new Coordinate(0,0)
        });
        service.floodZones = List.of(floodPolygon);

        LineString road = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(1,1),
                new Coordinate(3,3)
        });

        assertThat(service.isSafe(road)).isFalse();
    }
}
