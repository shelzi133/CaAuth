package z.caAuth.binding;

import z.caAuth.config.AuthConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CodeAttemptLimiter {

    private final AuthConfig authConfig;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    private record Attempts(int failures, long windowStartMillis, long lockedUntilMillis) {
    }

    public CodeAttemptLimiter(AuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    public long lockedMinutesLeft(String key) {
        Attempts entry = attempts.get(key);
        long now = System.currentTimeMillis();
        if (entry == null || entry.lockedUntilMillis <= now) {
            return 0;
        }
        return (entry.lockedUntilMillis - now + 59_999) / 60_000;
    }

    public void registerFailure(String key) {
        AuthConfig.BindingCode settings = authConfig.bots.bindingCode;
        if (settings.maxAttempts <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1, settings.attemptWindowMinutes) * 60_000L;
        long lockMillis = Math.max(1, settings.lockMinutes) * 60_000L;

        attempts.values().removeIf(entry -> entry.lockedUntilMillis <= now && now - entry.windowStartMillis > windowMillis);
        attempts.compute(key, (k, entry) -> {
            if (entry == null || now - entry.windowStartMillis > windowMillis) {
                entry = new Attempts(0, now, 0);
            }
            int failures = entry.failures + 1;
            if (failures >= settings.maxAttempts) {
                return new Attempts(0, now, now + lockMillis);
            }
            return new Attempts(failures, entry.windowStartMillis, 0);
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }
}
