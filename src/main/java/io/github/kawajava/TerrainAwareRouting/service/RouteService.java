package io.github.kawajava.TerrainAwareRouting.service;

import io.github.kawajava.TerrainAwareRouting.core.AStarPathFinder;
import io.github.kawajava.TerrainAwareRouting.core.PathFindingStrategy;
import io.github.kawajava.TerrainAwareRouting.core.SafeDijkstraPathFinder;
import io.github.kawajava.TerrainAwareRouting.domain.RoadSegment;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    private final PathFindingStrategy dijkstra = new SafeDijkstraPathFinder();
    private final PathFindingStrategy astar = new AStarPathFinder();

    @Value("${app.finding.value}")
    private String value;

    public List<Coordinate> computeRoute(
            List<RoadSegment> segments,
            Coordinate start,
            Coordinate end
    ) {
        PathFindingStrategy strategy = "astar".equalsIgnoreCase(value) ? astar : dijkstra;

        return strategy.findPath(segments, start, end);
    }
}