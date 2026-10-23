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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailCodeServiceTest {
    private static final String EMAIL = "friend@example.com";

    private MutableClock clock;
    private EmailCodeService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-23T08:00:00Z"));
        service = new EmailCodeService(true, clock);
    }

    @Test
    void correctCodeVerifiesOnce() {
        String code = service.createCode(EMAIL, "bind");

        assertDoesNotThrow(() -> service.verify(EMAIL, "bind", code));
        BusinessException reused = assertThrows(BusinessException.class, () -> service.verify(EMAIL, "bind", code));
        assertTrue(reused.getMessage().contains("过期"));
    }

    @Test
    void codeIsInvalidatedAfterTooManyWrongAttempts() {
        String code = service.createCode(EMAIL, "recover");
        String wrong = wrongCode(code);

        for (int i = 1; i < EmailCodeService.MAX_VERIFY_ATTEMPTS; i++) {
            BusinessException ex = assertThrows(BusinessException.class, () -> service.verify(EMAIL, "recover", wrong));
            assertTrue(ex.getMessage().contains("还可尝试 " + (EmailCodeService.MAX_VERIFY_ATTEMPTS - i) + " 次"));
        }
        BusinessException locked = assertThrows(BusinessException.class, () -> service.verify(EMAIL, "recover", wrong));
        assertTrue(locked.getMessage().contains("错误次数过多"));

        // 作废后即使输入正确验证码也无法通过
        BusinessException afterLock = assertThrows(BusinessException.class, () -> service.verify(EMAIL, "recover", code));
        assertTrue(afterLock.getMessage().contains("过期"));
    }

    @Test
    void resendWithinIntervalIsRejectedWith429() {
        service.createCode(EMAIL, "bind");
        clock.advance(Duration.ofSeconds(30));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createCode(EMAIL, "bind"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        assertTrue(ex.getMessage().contains("30 秒"));
    }

    @Test
    void resendAllowedAfterIntervalAndReplacesOldCode() {
        String first = service.createCode(EMAIL, "bind");
        clock.advance(EmailCodeService.RESEND_INTERVAL);

        String second = assertDoesNotThrow(() -> service.createCode(EMAIL, "bind"));

        if (!first.equals(second)) {
            assertThrows(BusinessException.class, () -> service.verify(EMAIL, "bind", first));
        }
        assertDoesNotThrow(() -> service.verify(EMAIL, "bind", second));
    }

    @Test
    void resendIntervalIsTrackedPerScene() {
        service.createCode(EMAIL, "bind");

        assertDoesNotThrow(() -> service.createCode(EMAIL, "recover"));
    }

    @Test
    void resendIntervalStillAppliesAfterSuccessfulVerification() {
        String code = service.createCode(EMAIL, "bind");
        service.verify(EMAIL, "bind", code);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createCode(EMAIL, "bind"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    @Test
    void codeExpiresAfterTtl() {
        String code = service.createCode(EMAIL, "bind");
        clock.advance(EmailCodeService.CODE_TTL);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.verify(EMAIL, "bind", code));
        assertTrue(ex.getMessage().contains("过期"));
    }

    @Test
    void emailIsNormalizedForLimits() {
        service.createCode("Friend@Example.com", "bind");

        assertThrows(BusinessException.class, () -> service.createCode(EMAIL, "bind"));
    }

    @Test
    void codeIsHiddenWhenDevReturnDisabled() {
        EmailCodeService production = new EmailCodeService(false, clock);

        assertNull(production.createCode(EMAIL, "bind"));
    }

    private String wrongCode(String code) {
        String wrong = code.equals("000000") ? "000001" : "000000";
        assertNotEquals(code, wrong);
        return wrong;
    }
}
