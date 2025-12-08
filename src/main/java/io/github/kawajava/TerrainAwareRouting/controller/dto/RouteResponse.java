package io.github.kawajava.TerrainAwareRouting.controller.dto;

import java.util.List;

public record RouteResponse(List<RouteStep> route, double totalCost) {}
