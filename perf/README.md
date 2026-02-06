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
REQUESTS=2000 \
CONCURRENCY=50 \
./scripts/load/run_event_load.sh
```

## Clean up
```bash
docker compose -f docker-compose.yml -f docker-compose.perf.yml down
```

## Results
Record a short summary in `perf/results.md`.
