package org.karthik;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class EventRelayTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "eventrelay.worker.enabled", "false",
                "eventrelay.worker.request-timeout", "200ms",
                "eventrelay.worker.retry-base-seconds", "0",
                "eventrelay.worker.retry-max-seconds", "0",
                "eventrelay.worker.retry-jitter-percent", "0",
                "eventrelay.worker.max-attempts", "2",
                "eventrelay.worker.max-in-flight-per-destination", "1",
                "eventrelay.worker.failure-threshold", "2",
                "eventrelay.worker.cooldown-seconds", "300"
        );
    }
}
