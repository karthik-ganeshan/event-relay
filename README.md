# event-relay

Event Relay is a small backend service that accepts events, stores them in Postgres, and delivers them to configured webhook destinations with retries, idempotency, and basic observability.

## Features
- REST API for destinations and events
- Durable storage in Postgres via Flyway migrations
- Idempotent event ingestion (per destination + idempotency key)
- Delivery worker with retry/backoff and max-attempt cutoff
- Safe claiming with SKIP LOCKED for concurrent workers
- HMAC signing for webhook deliveries (optional)
- Prometheus metrics and request correlation logging

## Prerequisites
- JDK 21 (non-Mandrel, via SDKMAN)
- Docker Desktop (daemon running)
- Maven Wrapper (./mvnw)

## Run in dev mode
```bash
./mvnw quarkus:dev -Ddebug=false
```

Swagger UI: http://localhost:8080/q/swagger-ui

## Enable delivery worker
By default the worker is disabled. To enable it in dev:
```bash
./mvnw quarkus:dev -Ddebug=false -Deventrelay.worker.enabled=true
```

## API examples
Create a destination:
```bash
curl -s -X POST http://localhost:8080/destinations \
  -H 'Content-Type: application/json' \
  -d '{"name":"DiscordBot","url":"https://example.com/webhook","authType":"HMAC","authSecret":"secret"}'
```

Create an event:
```bash
curl -s -X POST http://localhost:8080/events \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: req-123' \
  -d '{"destinationId":"DEST_ID","idempotencyKey":"abc-123","payload":{"userId":"123"}}'
```

Fetch event:
```bash
curl -s http://localhost:8080/events/EVENT_ID
```

List deliveries:
```bash
curl -s "http://localhost:8080/deliveries?eventId=EVENT_ID"
```

## HMAC webhook signing
If a destination has:
- `authType = "HMAC"`
- `authSecret` set

then deliveries include:
- `X-Event-Relay-Timestamp`: unix seconds
- `X-Event-Relay-Signature`: HMAC-SHA256 of
  `timestamp + "." + payload`

## Metrics
Prometheus metrics are exposed at:
```
http://localhost:8080/q/metrics
```
Look for counters prefixed with `eventrelay.`.

## Tests
Run integration tests:
```bash
./mvnw test
```

## Configuration
Worker settings (defaults in `application.properties`):
- `eventrelay.worker.enabled=false`
- `eventrelay.worker.batch-size=25`
- `eventrelay.worker.poll-interval=5s`
- `eventrelay.worker.request-timeout=5s`
- `eventrelay.worker.retry-base-seconds=5`
- `eventrelay.worker.retry-max-seconds=300`
- `eventrelay.worker.max-attempts=10`
