package io.github.kawajava.TerrainAwareRouting.domain;

import org.locationtech.jts.geom.LineString;

public record RoadSegment(String id, LineString geometry, double cost, boolean flooded) {}
