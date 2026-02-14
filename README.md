# event-relay

Event Relay is a Quarkus service that ingests events over HTTP, stores them in Postgres, and delivers them to webhook destinations using a background worker.

## What it has
- Postgres + Flyway migrations
- Idempotent event ingestion (per destination + idempotency key)
- Delivery worker with retries/backoff, jitter, cooldown, dead-lettering, redrive, attempt history
- Safe concurrent claiming using `FOR UPDATE SKIP LOCKED`
- Optional HMAC signing for webhook deliveries
- Admin API key + basic rate limiting
- Observability: Prometheus metrics, Grafana dashboard, structured logs, health checks

## Run (Docker)
Start the full local stack (app + db + Prometheus + Grafana):
```bash
docker compose up --build
```

URLs:
- API: `http://localhost:8080`
- Health: `http://localhost:8080/q/health`
- Metrics: `http://localhost:8080/q/metrics`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin/admin)

Defaults:
- Admin API key: `dev-admin-key` (sent as `X-API-Key`)
- Postgres: `localhost:5433` (`eventrelay` / `eventrelay`)

## Demo (end-to-end delivery)
Use the perf overlay to enable the worker and run a local echo webhook target:
```bash
docker compose -f docker-compose.yml -f docker-compose.perf.yml up --build -d
```

Then:
```bash
DEST_ID=$(curl -s -X POST http://localhost:8080/destinations \
  -H 'Content-Type: application/json' \
  -H 'X-API-Key: dev-admin-key' \
  -d '{"name":"Demo","url":"http://webhook:8080/webhook"}' | \
  python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

EVENT_ID=$(curl -s -X POST http://localhost:8080/events \
  -H 'Content-Type: application/json' \
  -H 'X-API-Key: dev-admin-key' \
  -d "{\"destinationId\":\"$DEST_ID\",\"payload\":{\"userId\":\"123\"}}" | \
  python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

curl -s "http://localhost:8080/deliveries?eventId=$EVENT_ID" \
  -H 'X-API-Key: dev-admin-key'
```

## Run (dev mode)
```bash
./mvnw quarkus:dev -Ddebug=false
```

Swagger UI (dev): `http://localhost:8080/q/swagger-ui`

## API (high level)
Admin endpoints require `X-API-Key`.
- `POST /destinations` (admin)
- `GET /destinations/{id}`
- `POST /events` (admin)
- `GET /events/{id}`
- `GET /deliveries?eventId=...` (admin)
- `GET /deliveries/{id}/attempts` (admin)
- `POST /deliveries/{id}/redrive` (admin)

## Observability
- Dashboard: `observability/grafana-dashboard.json` (auto-provisioned in Docker)
- Alert rules: `observability/alerts.yml`
- SLO notes: `docs/slo.md`

## Load testing
- How to run: `perf/README.md`
- Results: `perf/results.md`

## Tests
```bash
./mvnw test
```

