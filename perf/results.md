# Performance results

## Template (copy per run)
Test intent:
Environment:
Config:
Load profile:
Command:
Results:
- RPS:
- p50/p95/p99:
- error rate:
- delivery throughput:
- backlog drain time:
Artifacts:
- perf/artifacts/<run-id>-ingest-rate.png
- perf/artifacts/<run-id>-delivery-outcomes.png
- perf/artifacts/<run-id>-latency.png

## Run 1
Test intent:
Baseline high-load ingest with healthy delivery target
Environment:
MacBook Air, Docker Compose
Config:
- Worker enabled
- max-in-flight-per-destination=10
- batch-size=200
- poll-interval=1s
- rate limit: 10000/second
Load profile:
- destinations=20 (round-robin)
- requests=100000
- concurrency=200
- webhook target: local echo (200 OK)
Command:
```
BASE_URL=http://localhost:8080 \
API_KEY=dev-admin-key \
DEST_URL=http://webhook:8080/webhook \
DESTINATIONS=20 \
REQUESTS=100000 \
CONCURRENCY=200 \
./scripts/load/run_event_load.sh
```
Results:
- RPS: 2363.17
- p50/p95/p99: 0.0712 / 0.181 / 0.2704 seconds
- error rate: 0% (100000/100000 201)
- delivery throughput: see Grafana chart (steady plateau during backlog drain)
- backlog drain time: see Grafana chart (delivery rate falls to zero)
Raw output:
```
Requests: 100000
Elapsed(s): 42.316
RPS: 2363.17
Status codes: {'201': 100000}
Latency(s) p50/p95/p99/avg/max: 0.0712 0.181 0.2704 0.0835 0.6716
```
Artifacts:
- perf/artifacts/2026-02-06-run1-ingest-rate.png
- perf/artifacts/2026-02-06-run1-delivery-outcomes.png
- perf/artifacts/2026-02-06-run1-latency.png
