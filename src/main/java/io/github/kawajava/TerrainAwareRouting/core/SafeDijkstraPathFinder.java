package io.github.kawajava.TerrainAwareRouting.core;

import io.github.kawajava.TerrainAwareRouting.domain.RoadSegment;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;

import java.util.*;

@Slf4j
public class SafeDijkstraPathFinder implements PathFindingStrategy{

    public List<Coordinate> findPath (List<RoadSegment> segments, Coordinate start, Coordinate end) {

        Map<Coordinate, List<RoadSegment>> adjacency = new HashMap<>();

        for (RoadSegment seg : segments) {
            if (!seg.flooded()) {
                Coordinate from = seg.geometry().getStartPoint().getCoordinate();
                adjacency.computeIfAbsent(from, k -> new ArrayList<>()).add(seg);
            }
        }

        Map<Coordinate, Double> dist = new HashMap<>();
        Map<Coordinate, Coordinate> prev = new HashMap<>();
        PriorityQueue<Coordinate> queue = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

        dist.put(start, 0.0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Coordinate current = queue.poll();

            if (current.equals(end)) break;

            List<RoadSegment> edges = adjacency.getOrDefault(current, Collections.emptyList());
            for (RoadSegment seg : edges) {
                Coordinate neighbor = seg.geometry().getEndPoint().getCoordinate();
                double alt = dist.get(current) + seg.cost();

                if (alt < dist.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    dist.put(neighbor, alt);
                    prev.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        List<Coordinate> path = new ArrayList<>();
        Coordinate step = end;
        while (step != null) {
            path.add(step);
            step = prev.get(step);
        }
        Collections.reverse(path);

        log.info("Dijkstra path computed, {} steps", path.size());
        return path;
    }
}
