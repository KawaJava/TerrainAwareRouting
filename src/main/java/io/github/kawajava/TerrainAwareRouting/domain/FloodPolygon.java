package io.github.kawajava.TerrainAwareRouting.domain;

import org.locationtech.jts.geom.Polygon;

public record FloodPolygon(String id, Polygon polygon) {}
