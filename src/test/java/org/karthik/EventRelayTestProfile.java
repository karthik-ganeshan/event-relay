package org.karthik;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class EventRelayTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> overrides = new java.util.HashMap<>();
        overrides.put("eventrelay.worker.enabled", "false");
        overrides.put("eventrelay.worker.request-timeout", "200ms");
        overrides.put("eventrelay.worker.retry-base-seconds", "0");
        overrides.put("eventrelay.worker.retry-max-seconds", "0");
        overrides.put("eventrelay.worker.retry-jitter-percent", "0");
        overrides.put("eventrelay.worker.max-attempts", "2");
        overrides.put("eventrelay.worker.max-in-flight-per-destination", "1");
        overrides.put("eventrelay.worker.failure-threshold", "2");
        overrides.put("eventrelay.worker.cooldown-seconds", "300");
        return overrides;
    }
}
