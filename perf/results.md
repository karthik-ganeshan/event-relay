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
Date: 2026-02-06
Environment: MacBook Air, Docker Compose
Command:
```
BASE_URL=http://localhost:8080 \
API_KEY=dev-admin-key \
DEST_URL=http://webhook:8080/webhook \
REQUESTS=2000 \
CONCURRENCY=50 \
./scripts/load/run_event_load.sh
```

Output:
```
Requests: 2000
Elapsed(s): 2.366
RPS: 845.16
Status codes: {'201': 2000}
Latency(s) p50/p95/p99/avg/max: 0.0487 0.1249 0.3083 0.0586 0.4173
```

Notes:
- Worker enabled during run
- Destination points to local echo webhook

## Run 2
Date: 2026-02-06
Environment: MacBook Air, Docker Compose
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

Output:
```
Requests: 100000
Elapsed(s): 42.316
RPS: 2363.17
Status codes: {'201': 100000}
Latency(s) p50/p95/p99/avg/max: 0.0712 0.181 0.2704 0.0835 0.6716
```

Notes:
- Worker enabled during run
- 20 destinations, round-robin events
- Webhook echo target, no failures
