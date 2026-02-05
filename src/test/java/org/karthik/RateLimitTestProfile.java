package org.karthik;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class RateLimitTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "eventrelay.ratelimit.create.limit", "2",
                "eventrelay.ratelimit.create.window-seconds", "60"
        );
    }
}
