package io.github.kawajava.TerrainAwareRouting.core;

import io.github.kawajava.TerrainAwareRouting.domain.RoadSegment;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;

import java.util.*;

@Slf4j
public class SafeDijkstraPathFinder implements PathFindingStrategy {

    @Override
    public List<Coordinate> findPath(List<RoadSegment> segments, Coordinate start, Coordinate end) {

        Map<Coordinate, List<RoadSegment>> adjacency = buildAdjacencyMap(segments);
        Map<Coordinate, Double> dist = initializeDistanceMap(start);
        Map<Coordinate, Coordinate> prev = new HashMap<>();

        PriorityQueue<Coordinate> queue =
                new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        queue.add(start);

        while (!queue.isEmpty()) {
            Coordinate current = queue.poll();

            if (current.equals(end)) { break; }

            relaxEdges(current, adjacency, dist, prev, queue);
        }

        List<Coordinate> path = reconstructPath(prev, start, end);
        log.info("Dijkstra path computed, {} steps", path.size());

        return path;
    }

    public Map<Coordinate, List<RoadSegment>> buildAdjacencyMap(List<RoadSegment> segments) {
        Map<Coordinate, List<RoadSegment>> adjacency = new HashMap<>();

        segments.stream()
                .filter(seg -> !seg.flooded())
                .forEach(seg -> {
                    Coordinate from = extractCoordinate(seg, true);
                    adjacency.computeIfAbsent(from, k -> new ArrayList<>()).add(seg);
                });

        return adjacency;
    }

    public Map<Coordinate, Double> initializeDistanceMap(Coordinate start) {
        Map<Coordinate, Double> dist = new HashMap<>();
        dist.put(start, 0.0);
        return dist;
    }

    public void relaxEdges(Coordinate current,
                           Map<Coordinate, List<RoadSegment>> adjacency,
                           Map<Coordinate, Double> dist,
                           Map<Coordinate, Coordinate> prev,
                           PriorityQueue<Coordinate> queue) {

        List<RoadSegment> edges = adjacency.getOrDefault(current, Collections.emptyList());

        for (RoadSegment seg : edges) {
            Coordinate neighbor = extractNeighbor(seg);
            double alt = dist.get(current) + seg.cost();

            if (alt < dist.getOrDefault(neighbor, Double.MAX_VALUE)) {
                dist.put(neighbor, alt);
                prev.put(neighbor, current);
                queue.add(neighbor);
            }
        }
    }

    public List<Coordinate> reconstructPath(Map<Coordinate, Coordinate> prev,
                                            Coordinate start,
                                            Coordinate end) {

        if (!prev.containsKey(end) && !start.equals(end)) {
            return Collections.emptyList(); // brak ścieżki
        }

        List<Coordinate> path = new ArrayList<>();
        for (Coordinate step = end; step != null; step = prev.get(step)) {
            path.add(step);
        }

        Collections.reverse(path);
        return path;
    }

    public Coordinate extractCoordinate(RoadSegment seg, boolean startPoint) {
        return startPoint
                ? seg.geometry().getStartPoint().getCoordinate()
                : seg.geometry().getEndPoint().getCoordinate();
    }

    public Coordinate extractNeighbor(RoadSegment seg) {
        return extractCoordinate(seg, false);
    }
}
