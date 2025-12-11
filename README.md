# TerrainAwareRouting

## Description

`TerrainAwareRouting` is a Java Spring Boot application that computes evacuation routes while avoiding flooded or blocked roads. It uses OpenStreetMap data (GeoJSON format) and optional flood overlays to determine safe paths using Dijkstra or A* algorithms.

---

## Features

- Load road network from a GeoJSON file.
- Filter roads based on flood data.
- Compute optimal evacuation routes with Dijkstra or A*.
- REST endpoint returning route as GeoJSON with metadata.
- Layered architecture with services, controllers, and domain objects.
- Logging with different levels and categories.
- Exception handling with meaningful error responses.

---

## Getting Started

### Prerequisites

- Java 21
- Maven or Gradle
- IDE (IntelliJ, Eclipse, etc.)

### Running the Application

1. Clone the repository:

```bash
git clone <repo_url>
cd TerrainAwareRouting
```

2. Ensure `roads.geojson` is placed in `src/main/resources/`.

3. Build and run the application:

```bash
mvn clean install
mvn spring-boot:run
```

### Available Options

| Value     | Algorithm                      |
|-----------|---------------------------------|
| dijkstra  | classic shortest-path algorithm |
| astar     | A* algorithm with heuristic     |

### Example Configuration

```properties
# Select routing algorithm
app.finding.value=astar

# Path to the road network GeoJSON file
app.roads.geojson-path=roads.geojson
```

4. Access the REST endpoint:

```
GET http://localhost:8080/api/evac/route?start=52.2297,21.0122&end=52.2301,21.0133
```

- `start` and `end` are latitude,longitude

### Sample Response

```json
{
  "steps": [
    {"lat": 52.2297, "lon": 21.0122},
    {"lat": 52.2300, "lon": 21.0127},
    {"lat": 52.2301, "lon": 21.0133}
  ],
  "distance": 320.5
}
```

---

## Error Handling

Errors are returned in JSON format:

```json
{
    "timestamp": "2025-12-07T17:45:24.758Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Coordinates must be in format lat,lon",
    "path": "/api/evac/route"
}
```

---

## Project Structure

- `controller` - REST endpoints and DTOs
- `service` - Core services for routing and flood overlay
- `infrastructure` - Loaders and parsers (GeoJSON, flood)
- `domain` - Domain objects (`RoadSegment`, etc.)
- `core` - Pathfinding algorithms (Dijkstra, A*)
- `exception` - Own exceptions settings
- `test` - Unit tests

---

## Notes

- Designed for easy reuse of modules in other algorithms.
- Fully documented with logging and exception handling.
- Can run on any machine with Java 21 and Maven.

---
