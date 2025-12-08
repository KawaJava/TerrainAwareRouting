package io.github.kawajava.TerrainAwareRouting.core;

import io.github.kawajava.TerrainAwareRouting.domain.RoadSegment;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.InjectMocks;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AStarPathFinderTest {

    @InjectMocks
    private final AStarPathFinder pathFinder = new AStarPathFinder();

    private RoadSegment mockSegment(String id, Coordinate... coords) {
        LineString ls = mock(LineString.class);
        when(ls.getCoordinates()).thenReturn(coords);

        return new RoadSegment(id, ls, 1.0, false);
    }

    @Test
    void shouldReturnEmptyListCorrectly_whenNoSegments() {
        var start = new Coordinate(0, 0);
        var end = new Coordinate(1, 1);

        var path = pathFinder.findPath(List.of(), start, end);

        assertThat(path).isEmpty();
    }

    @Test
    void shouldFindDirectNeighborPathCorrectly() {
        var start = new Coordinate(0, 0);
        var mid = new Coordinate(0.0001, 0.0001);
        var end = new Coordinate(0.0002, 0.0002);

        var segment = mockSegment("s1", start, mid, end);

        var result = pathFinder.findPath(List.of(segment), start, end);

        assertThat(result).containsExactly(start, mid, end);
    }

    @Test
    void shouldFindShortestPathCorrectly_whenMultipleChoices() {
        var start = new Coordinate(0, 0);

        var wrong1 = new Coordinate(0.0001, 0.0);
        var wrong2 = new Coordinate(0.0002, 0.0);

        var good1 = new Coordinate(0.00005, 0.00005);
        var good2 = new Coordinate(0.0001,  0.0001);
        var end   = new Coordinate(0.00015, 0.00015);

        var segWrong = mockSegment("w1", start, wrong1, wrong2, end);
        var segGood  = mockSegment("g1", start, good1, good2, end);

        var result = pathFinder.findPath(List.of(segWrong, segGood), start, end);

        assertThat(result).containsExactly(start, good1, good2, end);
    }

    @Test
    void shouldNotRevisitClosedNodesCorrectly() {
        var start = new Coordinate(0, 0);
        var a = new Coordinate(0.0001, 0.0001);
        var b = new Coordinate(0.0002, 0.0002);
        var end = new Coordinate(0.0003, 0.0003);

        var loop = mockSegment("loop", start, a, b, end, start);

        var path = pathFinder.findPath(List.of(loop), start, end);

        assertThat(path).containsExactly(start, a, b, end);
    }

    @Test
    void shouldHandleCaseWhereStartEqualsEndCorrectly() {
        var start = new Coordinate(1, 2);

        var path = pathFinder.findPath(List.of(), start, start);

        assertThat(path).containsExactly(start);
    }

    @Test
    void shouldReturnCorrectNeighborsForChainedSegmentCorrectly() {
        var start = new Coordinate(0, 0);
        var mid   = new Coordinate(0.0001, 0.0001);
        var next  = new Coordinate(0.0002, 0.0002);
        var end   = new Coordinate(0.00029, 0.00029);

        var segment = mockSegment("s", start, mid, next, end);

        var neighbors = pathFinder.neighborsOf(start, List.of(segment));

        assertThat(neighbors)
                .containsExactlyInAnyOrder(mid, next)
                .doesNotContain(end);
    }
}
