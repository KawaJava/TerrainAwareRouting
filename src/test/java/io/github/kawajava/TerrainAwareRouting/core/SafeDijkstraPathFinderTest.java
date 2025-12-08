package io.github.kawajava.TerrainAwareRouting.core;

import io.github.kawajava.TerrainAwareRouting.domain.RoadSegment;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;

public class SafeDijkstraPathFinderTest {

    GeometryFactory gf = new GeometryFactory();
    SafeDijkstraPathFinder finder = new SafeDijkstraPathFinder();

    private RoadSegment segment(String id, Coordinate start, Coordinate end, double cost, boolean flooded) {
        LineString line = gf.createLineString(new Coordinate[]{start, end});
        return new RoadSegment(id, line, cost, flooded);
    }

    private RoadSegment safeSegment(String id, Coordinate start, Coordinate end, double cost) {
        return segment(id, start, end, cost, false);
    }

    private RoadSegment floodedSegment(String id, Coordinate start, Coordinate end) {
        return segment(id, start, end, 1.0, true);
    }

    @Test
    void shouldBuildAdjacencyMapCorrectly() {
        Coordinate a = new Coordinate(0, 0);
        Coordinate b = new Coordinate(1, 0);

        List<RoadSegment> segments = Arrays.asList(
                safeSegment("s1", a, b, 1.0),
                floodedSegment("f1", b, a) // should be ignored
        );

        Map<Coordinate, List<RoadSegment>> adj = finder.buildAdjacencyMap(segments);

        assertThat(adj).containsOnlyKeys(a);
        assertThat(adj.get(a)).hasSize(1);
        assertThat(adj.get(a).get(0).geometry().getEndPoint().getCoordinate()).isEqualTo(b);
    }

    @Test
    void shouldInitializeDistanceMapCorrectly() {
        Coordinate start = new Coordinate(5, 5);
        Map<Coordinate, Double> dist = finder.initializeDistanceMap(start);

        assertThat(dist).containsEntry(start, 0.0);
        assertThat(dist).hasSize(1);
    }

    @Test
    void shouldExtractCoordinateCorrectly() {
        Coordinate a = new Coordinate(0, 0);
        Coordinate b = new Coordinate(1, 1);

        RoadSegment seg = safeSegment("s1", a, b, 1.0);

        assertThat(finder.extractCoordinate(seg, true)).isEqualTo(a);
        assertThat(finder.extractCoordinate(seg, false)).isEqualTo(b);
    }

    @Test
    void shouldExtractNeighborCorrectly() {
        Coordinate a = new Coordinate(0, 0);
        Coordinate b = new Coordinate(1, 1);

        RoadSegment seg = safeSegment("s1", a, b, 1.0);

        assertThat(finder.extractNeighbor(seg)).isEqualTo(b);
    }

    @Test
    void shouldRelaxEdgesCorrectly() {
        Coordinate a = new Coordinate(0, 0);
        Coordinate b = new Coordinate(1, 0);

        RoadSegment seg = safeSegment("s1", a, b, 2.0);

        Map<Coordinate, List<RoadSegment>> adj = new HashMap<>();
        adj.put(a, List.of(seg));

        Map<Coordinate, Double> dist = new HashMap<>();
        dist.put(a, 0.0);

        Map<Coordinate, Coordinate> prev = new HashMap<>();
        PriorityQueue<Coordinate> queue = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        queue.add(a);

        finder.relaxEdges(a, adj, dist, prev, queue);

        assertThat(dist).containsEntry(b, 2.0);
        assertThat(prev).containsEntry(b, a);
        assertThat(queue).contains(b);
    }

    @Test
    void shouldReconstructPathCorrectly() {
        Coordinate a = new Coordinate(0, 0);
        Coordinate b = new Coordinate(1, 0);
        Coordinate c = new Coordinate(2, 0);

        Map<Coordinate, Coordinate> prev = new HashMap<>();
        prev.put(c, b);
        prev.put(b, a);

        List<Coordinate> path = finder.reconstructPath(prev, a, c);

        assertThat(path).containsExactly(a, b, c);
    }

    @Test
    void shouldReturnEmptyPathWhenNoPathExists() {
        Coordinate a = new Coordinate(0, 0);
        Coordinate b = new Coordinate(1, 0);

        Map<Coordinate, Coordinate> prev = new HashMap<>();

        List<Coordinate> path = finder.reconstructPath(prev, a, b);

        assertThat(path).isEmpty();
    }

    @Test
    void shouldFindPathCorrectly() {
        Coordinate a = new Coordinate(0, 0);
        Coordinate b = new Coordinate(1, 0);
        Coordinate c = new Coordinate(2, 0);

        List<RoadSegment> segments = Arrays.asList(
                safeSegment("s1", a, b, 1),
                safeSegment("s2", b, c, 1)
        );

        List<Coordinate> path = finder.findPath(segments, a, c);

        assertThat(path).containsExactly(a, b, c);
    }

    @Test
    void shouldAvoidFloodedSegmentsCorrectly() {
        Coordinate a = new Coordinate(0, 0);
        Coordinate b = new Coordinate(1, 0);

        List<RoadSegment> segments = List.of(
                floodedSegment("f1", a, b)
        );

        List<Coordinate> path = finder.findPath(segments, a, b);

        assertThat(path).isEmpty();
    }
}
