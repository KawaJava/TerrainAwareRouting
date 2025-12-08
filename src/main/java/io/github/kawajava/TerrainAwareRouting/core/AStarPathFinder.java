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
        var open = new PriorityQueue<Node>(Comparator.comparingDouble(Node::f));
        var closed = new HashSet<Coordinate>();
        var gScore = new HashMap<Coordinate, Double>();
        var allNodes = new HashMap<Coordinate, Node>();

        initializeStartNode(start, end, open, gScore, allNodes);

        while (!open.isEmpty()) {
            var current = open.poll();

            if (isGoal(current.coord(), end)) {
                return reconstruct(current);
            }

            closed.add(current.coord());

            for (var neighbor : neighborsOf(current.coord(), segments)) {
                processNeighbor(current, neighbor, end,
                        open, closed, gScore, allNodes);
            }
        }

        return List.of();
    }

    private void initializeStartNode(
            Coordinate start, Coordinate end,
            PriorityQueue<Node> open,
            Map<Coordinate, Double> gScore,
            Map<Coordinate, Node> allNodes
    ) {
        var startNode = new Node(start, 0, heuristic(start, end), null);
        open.add(startNode);
        gScore.put(start, 0.0);
        allNodes.put(start, startNode);
    }

    private boolean isGoal(Coordinate a, Coordinate b) {
        return a.equals2D(b);
    }

    private void processNeighbor(
            Node current,
            Coordinate neighbor,
            Coordinate end,

            PriorityQueue<Node> open,
            Set<Coordinate> closed,
            Map<Coordinate, Double> gScore,
            Map<Coordinate, Node> allNodes
    ) {
        if (shouldSkipNeighbor(neighbor, closed)) return;

        double tentativeG = current.g() + current.coord().distance(neighbor);

        if (tentativeG >= gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) return;

        var neighborNode = new Node(
                neighbor,
                tentativeG,
                heuristic(neighbor, end),
                current
        );

        gScore.put(neighbor, tentativeG);
        allNodes.put(neighbor, neighborNode);
        open.add(neighborNode);
    }

    private boolean shouldSkipNeighbor(Coordinate neighbor, Set<Coordinate> closed) {
        return closed.contains(neighbor);
    }

    private double heuristic(Coordinate a, Coordinate b) {
        return a.distance(b);
    }

    List<Coordinate> neighborsOf(Coordinate c, List<RoadSegment> segments) {
        return segments.stream()
                .flatMap(seg -> Arrays.stream(seg.geometry().getCoordinates()))
                .filter(coord -> !coord.equals2D(c))
                .filter(coord -> coord.distance(c) < 0.0003)
                .toList();
    }

    private List<Coordinate> reconstruct(Node end) {
        var path = new ArrayList<Coordinate>();
        var current = end;

        while (current != null) {
            path.add(current.coord());
            current = current.parent();
        }

        Collections.reverse(path);
        return path;
    }
}
