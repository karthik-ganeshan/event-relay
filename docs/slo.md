# Service level objectives

These are initial targets for the event-relay service. Adjust after you have
baseline data from production traffic.

## SLO 1: Event ingestion availability
Target: 99.9% successful POST /events over 30 days

PromQL:
```
sum(rate(http_server_requests_seconds_count{uri="/events",outcome="SUCCESS"}[5m]))
/ sum(rate(http_server_requests_seconds_count{uri="/events"}[5m]))
```

## SLO 2: Delivery success rate
Target: 99.0% of deliveries succeed within max attempts

PromQL:
```
sum(rate(eventrelay_deliveries_delivered_total[5m]))
/ sum(rate(eventrelay_deliveries_outcome_total[5m]))
```

## SLO 3: Delivery latency
Target: p95 under 2 seconds

PromQL (histogram enabled):
```
histogram_quantile(0.95, sum(rate(eventrelay_deliveries_latency_seconds_bucket[5m])) by (le))
```

Fallback average latency:
```
rate(eventrelay_deliveries_latency_seconds_sum[5m])
/ rate(eventrelay_deliveries_latency_seconds_count[5m])
```

## Error budgets
- Event ingestion: 0.1% over 30 days
- Delivery success: 1.0% over 30 days

## Notes
- Label keys on `http_server_requests_seconds_*` can vary by Quarkus version
- Use longer windows for quarterly or annual reporting
