#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}
API_KEY=${API_KEY:-dev-admin-key}
REQUESTS=${REQUESTS:-500}
CONCURRENCY=${CONCURRENCY:-20}
DEST_URL=${DEST_URL:-http://localhost:65535/webhook}

create_destination() {
  local name
  name="LoadTest-$(date +%s)"
  curl -s -o "$1" -w "%{http_code}" -X POST "$BASE_URL/destinations" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $API_KEY" \
    -d "{\"name\":\"$name\",\"url\":\"$DEST_URL\"}"
}

dest_tmp=$(mktemp)
dest_code=$(create_destination "$dest_tmp")
dest_body=$(cat "$dest_tmp")

dest_id=$(python3 - <<'PY' "$dest_tmp"
import json
import sys
with open(sys.argv[1], "r", encoding="utf-8") as handle:
    data = json.load(handle)
print(data.get("id", ""))
PY
)

rm -f "$dest_tmp"

if [ -z "$dest_id" ]; then
  echo "Failed to create destination (status $dest_code)" >&2
  echo "$dest_body" >&2
  exit 1
fi

python3 - <<'PY' "$BASE_URL" "$API_KEY" "$dest_id" "$REQUESTS" "$CONCURRENCY"
import json
import statistics
import sys
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed

base_url = sys.argv[1]
api_key = sys.argv[2]
dest_id = sys.argv[3]
request_count = int(sys.argv[4])
concurrency = int(sys.argv[5])

url = base_url.rstrip("/") + "/events"

def send_request(i):
    payload = {"destinationId": dest_id, "payload": {"userId": f"user-{i}"}}
    body = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("X-API-Key", api_key)
    start = time.perf_counter()
    status = 0
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            resp.read()
            status = resp.getcode()
    except Exception:
        status = 0
    elapsed = time.perf_counter() - start
    return status, elapsed

start = time.perf_counter()
statuses = {}
times = []

with ThreadPoolExecutor(max_workers=concurrency) as executor:
    futures = [executor.submit(send_request, i) for i in range(1, request_count + 1)]
    for future in as_completed(futures):
        status, elapsed = future.result()
        statuses[str(status)] = statuses.get(str(status), 0) + 1
        times.append(elapsed)

elapsed_total = max(time.perf_counter() - start, 1e-6)
times.sort()

def percentile(values, pct):
    if not values:
        return 0.0
    k = int(round((pct / 100.0) * (len(values) - 1)))
    return values[k]

p50 = percentile(times, 50)
p95 = percentile(times, 95)
p99 = percentile(times, 99)
avg = statistics.mean(times) if times else 0.0
max_time = max(times) if times else 0.0
rps = request_count / elapsed_total

print("Requests:", request_count)
print("Elapsed(s):", round(elapsed_total, 3))
print("RPS:", round(rps, 2))
print("Status codes:", statuses)
print("Latency(s) p50/p95/p99/avg/max:", round(p50, 4), round(p95, 4), round(p99, 4), round(avg, 4), round(max_time, 4))
PY
