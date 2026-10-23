package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败次数限制：同一“用户名 + IP”在统计窗口内连续失败达到上限后锁定一段时间。
 * 按用户名 + IP 计数，避免攻击者仅凭用户名就能锁住真实用户。
 * 数据保存在内存中，重启后清空；多实例部署需要换成 Redis 等共享存储。
 */
@Service
public class LoginAttemptService {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String username, String clientIp) {
        Attempt attempt = attempts.get(key(username, clientIp));
        Instant now = clock.instant();
        if (attempt != null && attempt.lockedUntil() != null && now.isBefore(attempt.lockedUntil())) {
            long seconds = Duration.between(now, attempt.lockedUntil()).toSeconds();
            long minutes = Math.max(1, (seconds + 59) / 60);
            throw BusinessException.tooManyRequests("登录失败次数过多，请 " + minutes + " 分钟后再试");
        }
    }

    public void recordFailure(String username, String clientIp) {
        Instant now = clock.instant();
        attempts.compute(key(username, clientIp), (k, current) -> {
            if (current == null || current.isStale(now)) {
                return new Attempt(1, now, null);
            }
            int failures = current.failures() + 1;
            Instant lockedUntil = failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;
            return new Attempt(failures, current.firstFailureAt(), lockedUntil);
        });
        if (attempts.size() > CLEANUP_THRESHOLD) {
            attempts.values().removeIf(attempt -> attempt.isStale(now));
        }
    }

    public void recordSuccess(String username, String clientIp) {
        attempts.remove(key(username, clientIp));
    }

    private String key(String username, String clientIp) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        return normalized + "|" + clientIp;
    }

    private record Attempt(int failures, Instant firstFailureAt, Instant lockedUntil) {
        /** 锁定已结束，或未锁定但距首次失败已超过统计窗口 */
        boolean isStale(Instant now) {
            if (lockedUntil != null) {
                return !now.isBefore(lockedUntil);
            }
            return !now.isBefore(firstFailureAt.plus(WINDOW));
        }
    }
}
