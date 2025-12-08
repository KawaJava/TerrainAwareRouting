package io.github.kawajava.TerrainAwareRouting.core;

import io.github.kawajava.TerrainAwareRouting.domain.RoadSegment;
import org.locationtech.jts.geom.Coordinate;

import java.util.List;

public interface PathFindingStrategy {
    List<Coordinate> findPath(List<RoadSegment> segments, Coordinate start, Coordinate end);
}
