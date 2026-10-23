package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailCodeService {
    static final Duration CODE_TTL = Duration.ofMinutes(10);
    static final Duration RESEND_INTERVAL = Duration.ofSeconds(60);
    static final int MAX_VERIFY_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, CodeRecord> codes = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastSentAt = new ConcurrentHashMap<>();
    private final boolean devReturnCode;
    private final Clock clock;

    @Autowired
    public EmailCodeService(@Value("${app.email.dev-return-code:true}") boolean devReturnCode) {
        this(devReturnCode, Clock.systemUTC());
    }

    EmailCodeService(boolean devReturnCode, Clock clock) {
        this.devReturnCode = devReturnCode;
        this.clock = clock;
    }

    public String createCode(String email, String scene) {
        String key = key(normalizeEmail(email), scene);
        Instant now = clock.instant();
        purgeExpired(now);

        Instant previous = lastSentAt.get(key);
        if (previous != null && now.isBefore(previous.plus(RESEND_INTERVAL))) {
            long wait = (Duration.between(now, previous.plus(RESEND_INTERVAL)).toMillis() + 999) / 1000;
            throw BusinessException.tooManyRequests("验证码发送过于频繁，请 " + wait + " 秒后再试");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.put(key, new CodeRecord(code, now.plus(CODE_TTL), 0));
        lastSentAt.put(key, now);

        // This project has no SMTP configuration yet. In production, send this code by email here.
        return devReturnCode ? code : null;
    }

    public void verify(String email, String scene, String code) {
        String key = key(normalizeEmail(email), scene);
        Instant now = clock.instant();
        CodeRecord record = codes.get(key);
        if (record == null || !now.isBefore(record.expiresAt())) {
            codes.remove(key);
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (code != null && record.code().equals(code.trim())) {
            codes.remove(key);
            return;
        }

        // 输错次数达到上限后作废验证码，防止暴力枚举 6 位数字
        CodeRecord updated = codes.computeIfPresent(key, (k, current) -> {
            int failed = current.failedAttempts() + 1;
            return failed >= MAX_VERIFY_ATTEMPTS
                    ? null
                    : new CodeRecord(current.code(), current.expiresAt(), failed);
        });
        if (updated == null) {
            throw new BusinessException("验证码错误次数过多，请重新获取");
        }
        int remaining = MAX_VERIFY_ATTEMPTS - updated.failedAttempts();
        throw new BusinessException("验证码不正确，还可尝试 " + remaining + " 次");
    }

    public String normalizeEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BusinessException("请输入有效邮箱地址");
        }
        return email.trim().toLowerCase();
    }

    private void purgeExpired(Instant now) {
        codes.values().removeIf(record -> !now.isBefore(record.expiresAt()));
        lastSentAt.values().removeIf(sentAt -> !now.isBefore(sentAt.plus(RESEND_INTERVAL)));
    }

    private String key(String email, String scene) {
        return scene + ":" + email;
    }

    private record CodeRecord(String code, Instant expiresAt, int failedAttempts) {
    }
}
