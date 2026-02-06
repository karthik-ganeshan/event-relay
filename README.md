# event-relay

Event Relay is a small backend service that accepts events, stores them in Postgres, and delivers them to configured webhook destinations with retries, idempotency, and production-style reliability controls.

## Features
- REST API for destinations and events
- Durable storage in Postgres via Flyway migrations
- Idempotent event ingestion (per destination + idempotency key)
- Delivery worker with retry/backoff, jitter, and max-attempt cutoff
- Safe claiming with SKIP LOCKED for concurrent workers
- Per-destination concurrency limits and cooldown circuit breaker
- HMAC signing for webhook deliveries (optional)
- Attempt history and redrive for failed deliveries
- API key auth + rate limits on admin endpoints
- Prometheus metrics and request correlation logging

## Architecture
```
Clients
  |
  v
Event Relay API  --->  Postgres (events, deliveries, attempts)
  |                      ^
  |                      |
  v                      |
Delivery Worker ---------+
  |
  v
Webhook Destinations

Observability: logs + /q/metrics + /q/health
```

## Prerequisites
- JDK 21 (non-Mandrel, via SDKMAN)
- Docker Desktop (daemon running)
- Maven Wrapper (./mvnw)

## Run in dev mode
```bash
./mvnw quarkus:dev -Ddebug=false
```

Swagger UI: http://localhost:8080/q/swagger-ui

## Run with Docker Compose
```bash
docker compose up --build
```

Default API key in `docker-compose.yml`: `dev-admin-key`
Postgres is exposed on host port `5433`

## GCP Terraform
Terraform scaffolding for Cloud Run + Cloud SQL lives in `infra/gcp`

## GCP deploy
After provisioning infra, you can deploy from GitHub Actions using the
`Deploy to Cloud Run` workflow and the secrets listed in `infra/gcp/README.md`

## Demo flow (Docker)
In another terminal:
```bash
DEST_ID=$(curl -s -X POST http://localhost:8080/destinations \
  -H 'Content-Type: application/json' \
  -H 'X-API-Key: dev-admin-key' \
  -d '{"name":"Demo","url":"https://example.com/webhook"}' | \
  python -c 'import json,sys; print(json.load(sys.stdin)["id"])')

EVENT_ID=$(curl -s -X POST http://localhost:8080/events \
  -H 'Content-Type: application/json' \
  -H 'X-API-Key: dev-admin-key' \
  -d "{\"destinationId\":\"$DEST_ID\",\"payload\":{\"userId\":\"123\"}}" | \
  python -c 'import json,sys; print(json.load(sys.stdin)["id"])')

curl -s "http://localhost:8080/deliveries?eventId=$EVENT_ID" \
  -H 'X-API-Key: dev-admin-key'
```

## Security
Admin endpoints require an API key via `X-API-Key`:
- `POST /destinations`
- `POST /events`
- `GET /deliveries`
- `GET /deliveries/{id}/attempts`
- `POST /deliveries/{id}/redrive`

Default dev key in `application.properties`: `dev-admin-key`

## Limits
- Request body size: `1M`
- Create endpoint rate limit defaults: 60 requests / 60 seconds

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
  -H 'X-API-Key: dev-admin-key' \
  -d '{"name":"DiscordBot","url":"https://example.com/webhook","authType":"HMAC","authSecret":"secret"}'
```

Create an event:
```bash
curl -s -X POST http://localhost:8080/events \
  -H 'Content-Type: application/json' \
  -H 'X-API-Key: dev-admin-key' \
  -H 'X-Request-Id: req-123' \
  -d '{"destinationId":"DEST_ID","idempotencyKey":"abc-123","payload":{"userId":"123"}}'
```

Fetch event:
```bash
curl -s http://localhost:8080/events/EVENT_ID
```

List deliveries:
```bash
curl -s "http://localhost:8080/deliveries?eventId=EVENT_ID" \
  -H 'X-API-Key: dev-admin-key'
```

List delivery attempts:
```bash
curl -s "http://localhost:8080/deliveries/DELIVERY_ID/attempts" \
  -H 'X-API-Key: dev-admin-key'
```

Redrive failed delivery:
```bash
curl -s -X POST "http://localhost:8080/deliveries/DELIVERY_ID/redrive" \
  -H 'X-API-Key: dev-admin-key'
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

## Observability
Alert rules:
- `observability/alerts.yml`

Grafana dashboard:
- `observability/grafana-dashboard.json`

SLO targets and PromQL:
- `docs/slo.md`

Sample PromQL:
```
sum(rate(eventrelay_events_created_total[1m]))
sum(rate(eventrelay_deliveries_delivered_total[1m]))
rate(eventrelay_deliveries_latency_seconds_sum[5m]) / rate(eventrelay_deliveries_latency_seconds_count[5m])
```

### Prometheus + Grafana (Docker)
Start the stack:
```bash
docker compose up --build
```

Prometheus UI:
```
http://localhost:9090
```

Grafana UI:
```
http://localhost:3000
```

Grafana login:
- user: `admin`
- password: `admin`

The dashboard named `Event Relay` is auto-provisioned from:
`observability/grafana-dashboard.json`

## Tracing (OpenTelemetry)
Tracing is disabled by default. To enable OTLP export:
```bash
export OTEL_TRACES_EXPORTER=otlp
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
```

For GCP, run an OpenTelemetry Collector that exports to Cloud Trace and point
`OTEL_EXPORTER_OTLP_ENDPOINT` to the collector.

## Health checks
Liveness and readiness endpoints:
```
http://localhost:8080/q/health
http://localhost:8080/q/health/live
http://localhost:8080/q/health/ready
```

## Load test (local)
Run the service with a relaxed rate limit profile:
```bash
QUARKUS_PROFILE=loadtest ./mvnw quarkus:dev -Ddebug=false
```

In another terminal:
```bash
BASE_URL=http://localhost:8080 REQUESTS=500 CONCURRENCY=20 ./scripts/load/run_event_load.sh
```

Sample result on a local machine:
```
Requests: 500
Elapsed(s): 0.631
RPS: 792.9
Status codes: {'201': 500}
Latency(s) p50/p95/p99/avg/max: 0.0151 0.0391 0.298 0.0249 0.5332
```

## Tests
Run integration tests:
```bash
./mvnw test
```

## Runbook
Common failure scenarios:
- Destination down: delivery retries with backoff + jitter
- Max attempts reached: delivery marked FAILED (dead-lettered)
- Destination repeatedly failing: destination enters cooldown

Recovery:
- Use `POST /deliveries/{id}/redrive` to requeue a FAILED delivery
- Review attempt history via `GET /deliveries/{id}/attempts`
- Check alert rules in `observability/alerts.yml` and SLO targets in `docs/slo.md`

## Configuration
Worker settings (defaults in `application.properties`):
- `eventrelay.worker.enabled=false`
- `eventrelay.worker.batch-size=25`
- `eventrelay.worker.poll-interval=5s`
- `eventrelay.worker.request-timeout=5s`
- `eventrelay.worker.retry-base-seconds=5`
- `eventrelay.worker.retry-max-seconds=300`
- `eventrelay.worker.retry-jitter-percent=20`
- `eventrelay.worker.max-attempts=10`
- `eventrelay.worker.max-in-flight-per-destination=2`
- `eventrelay.worker.failure-threshold=3`
- `eventrelay.worker.cooldown-seconds=60`
