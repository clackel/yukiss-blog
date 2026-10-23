package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import moon.yukiss.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {
    private static final String IP = "203.0.113.7";

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-23T08:00:00Z"));
        service = new LoginAttemptService(clock);
    }

    @Test
    void locksAfterMaxFailures() {
        failTimes("alice", IP, LoginAttemptService.MAX_FAILURES - 1);
        assertDoesNotThrow(() -> service.checkAllowed("alice", IP));

        service.recordFailure("alice", IP);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.checkAllowed("alice", IP));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertTrue(ex.getMessage().contains("15 分钟"));
    }

    @Test
    void lockExpiresAfterLockDuration() {
        failTimes("alice", IP, LoginAttemptService.MAX_FAILURES);
        clock.advance(LoginAttemptService.LOCK_DURATION.minusSeconds(1));
        assertThrows(BusinessException.class, () -> service.checkAllowed("alice", IP));

        clock.advance(Duration.ofSeconds(1));

        assertDoesNotThrow(() -> service.checkAllowed("alice", IP));
        // 解锁后重新计数，一次失败不会立即再次锁定
        service.recordFailure("alice", IP);
        assertDoesNotThrow(() -> service.checkAllowed("alice", IP));
    }

    @Test
    void successResetsFailureCount() {
        failTimes("alice", IP, LoginAttemptService.MAX_FAILURES - 1);
        service.recordSuccess("alice", IP);
        failTimes("alice", IP, LoginAttemptService.MAX_FAILURES - 1);

        assertDoesNotThrow(() -> service.checkAllowed("alice", IP));
    }

    @Test
    void failuresOutsideWindowAreForgotten() {
        failTimes("alice", IP, LoginAttemptService.MAX_FAILURES - 1);
        clock.advance(LoginAttemptService.WINDOW);
        service.recordFailure("alice", IP);

        assertDoesNotThrow(() -> service.checkAllowed("alice", IP));
    }

    @Test
    void lockIsScopedToUsernameAndIp() {
        failTimes("alice", IP, LoginAttemptService.MAX_FAILURES);

        // 其他 IP 上的真实用户不受影响，同 IP 的其他用户名也不受影响
        assertDoesNotThrow(() -> service.checkAllowed("alice", "198.51.100.2"));
        assertDoesNotThrow(() -> service.checkAllowed("bob", IP));
    }

    @Test
    void usernameIsCaseInsensitive() {
        failTimes("Alice", IP, LoginAttemptService.MAX_FAILURES);

        assertThrows(BusinessException.class, () -> service.checkAllowed("alice", IP));
    }

    private void failTimes(String username, String ip, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(username, ip);
        }
    }
}
