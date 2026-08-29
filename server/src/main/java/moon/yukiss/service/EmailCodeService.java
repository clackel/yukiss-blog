package moon.yukiss.service;

import moon.yukiss.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, CodeRecord> codes = new ConcurrentHashMap<>();

    @Value("${app.email.dev-return-code:true}")
    private boolean devReturnCode;

    public String createCode(String email, String scene) {
        String normalizedEmail = normalizeEmail(email);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.put(key(normalizedEmail, scene), new CodeRecord(code, Instant.now().plusSeconds(600)));

        // This project has no SMTP configuration yet. In production, send this code by email here.
        return devReturnCode ? code : null;
    }

    public void verify(String email, String scene, String code) {
        String normalizedEmail = normalizeEmail(email);
        CodeRecord record = codes.get(key(normalizedEmail, scene));
        if (record == null || record.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (code == null || !record.code().equals(code.trim())) {
            throw new BusinessException("验证码不正确");
        }
        codes.remove(key(normalizedEmail, scene));
    }

    public String normalizeEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BusinessException("请输入有效邮箱地址");
        }
        return email.trim().toLowerCase();
    }

    private String key(String email, String scene) {
        return scene + ":" + email;
    }

    private record CodeRecord(String code, Instant expiresAt) {
    }
}
