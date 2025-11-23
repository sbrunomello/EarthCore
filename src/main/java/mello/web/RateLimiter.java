package mello.web;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple fixed-window rate limiter per IP for the embedded HTTP server.
 */
public class RateLimiter {

    private final int limitPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    public boolean allow(String key) {
        long currentMinute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new Window(currentMinute, 1);
            }
            if (existing.count >= limitPerMinute) {
                return existing;
            }
            return new Window(currentMinute, existing.count + 1);
        });
        return window.count <= limitPerMinute;
    }

    private record Window(long minute, int count) {}
}
