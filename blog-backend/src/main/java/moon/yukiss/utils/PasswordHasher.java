package moon.yukiss.utils;

import moon.yukiss.common.BusinessException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordHasher {
    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String hash(String password) {
        validatePassword(password);
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$" + encode(salt) + "$" + encode(derived);
    }

    public boolean matches(String password, String stored) {
        if (password == null || stored == null || !stored.startsWith(PREFIX + "$")) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4) {
            return false;
        }
        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = decode(parts[2]);
        byte[] expected = decode(parts[3]);
        byte[] actual = derive(password, salt, iterations);
        return constantTimeEquals(expected, actual);
    }

    public boolean needsUpgrade(String stored) {
        return stored == null || !stored.startsWith(PREFIX + "$");
    }

    public void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("密码至少需要 8 位");
        }
        if (password.length() > 72) {
            throw new BusinessException("密码不能超过 72 位");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException("密码需要同时包含字母和数字");
        }
    }

    private byte[] derive(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash password", ex);
        }
    }

    private boolean constantTimeEquals(byte[] left, byte[] right) {
        if (left.length != right.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length; i++) {
            result |= left[i] ^ right[i];
        }
        return result == 0;
    }

    private String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private byte[] decode(String value) {
        return Base64.getDecoder().decode(value);
    }
}
