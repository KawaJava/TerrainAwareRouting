package io.github.kawajava.TerrainAwareRouting.core;

import io.github.kawajava.TerrainAwareRouting.domain.RoadSegment;
import org.locationtech.jts.geom.Coordinate;

import java.util.*;

public class AStarPathFinder implements PathFindingStrategy {

    private record Node(Coordinate coord, double g, double h, Node parent) {
        double f() { return g + h; }
    }

    @Override
    public List<Coordinate> findPath(
            List<RoadSegment> segments,
            Coordinate start,
            Coordinate end
    ) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        Set<Coordinate> closed = new HashSet<>();

        Map<Coordinate, Double> gScore = new HashMap<>();
        Map<Coordinate, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, 0, heuristic(start, end), null);
        open.add(startNode);
        gScore.put(start, 0.0);
        allNodes.put(start, startNode);

        while (!open.isEmpty()) {
            Node current = open.poll();

            if (current.coord().equals2D(end)) {return reconstruct(current); }

            closed.add(current.coord());

            for (Coordinate neighbor : neighborsOf(current.coord(), segments)) {
                if (closed.contains(neighbor)) continue;

                double tentativeG = current.g() + current.coord().distance(neighbor);

                if (tentativeG >= gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) continue;

                Node neighborNode = new Node(neighbor, tentativeG, heuristic(neighbor, end), current);

                gScore.put(neighbor, tentativeG);
                allNodes.put(neighbor, neighborNode);

                open.add(neighborNode);
            }
        }

        return List.of();
    }

    private double heuristic(Coordinate a, Coordinate b) {
        return a.distance(b);
    }

    private List<Coordinate> neighborsOf(Coordinate c, List<RoadSegment> segments) {
        return segments.stream()
                .flatMap(seg -> Arrays.stream(seg.geometry().getCoordinates()))
                .filter(coord -> !coord.equals2D(c))
                .filter(coord -> coord.distance(c) < 0.0003)
                .toList();
    }
    private List<Coordinate> reconstruct(Node end) {
        List<Coordinate> path = new ArrayList<>();
        Node current = end;

        while (current != null) {
            path.add(current.coord());
            current = current.parent();
        }

        Collections.reverse(path);
        return path;
    }
}
