# Load testing

This project uses a lightweight script (`scripts/load/run_event_load.sh`) to
stress the event ingestion path.

## Start services
Use the perf overlay to enable the worker, widen rate limits, and add a webhook
endpoint that always returns 200.

```bash
docker compose -f docker-compose.yml -f docker-compose.perf.yml up --build -d
```

## Run the load test
```bash
BASE_URL=http://localhost:8080 \
API_KEY=dev-admin-key \
DEST_URL=http://webhook:8080/webhook \
DESTINATIONS=20 \
REQUESTS=100000 \
CONCURRENCY=200 \
./scripts/load/run_event_load.sh
```

## Scaling test (more destinations + larger batch)
```bash
docker compose -f docker-compose.yml -f docker-compose.perf.yml -f docker-compose.scale.yml up --build -d

BASE_URL=http://localhost:8080 \
API_KEY=dev-admin-key \
DEST_URL=http://webhook:8080/webhook \
DESTINATIONS=50 \
REQUESTS=100000 \
CONCURRENCY=200 \
./scripts/load/run_event_load.sh
```

## Clean up
```bash
docker compose -f docker-compose.yml -f docker-compose.perf.yml down
```

## Results
Record a short summary in `perf/results.md`.
Store Grafana screenshots in `perf/artifacts/` using a run-id prefix, for example:
`2026-02-06-run2-ingest-rate.png`.
