# Performance results

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
