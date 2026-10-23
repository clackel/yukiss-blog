package moon.yukiss.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    private final String secret;
    private final long expirationHours;

    public JwtUtils(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-hours}") long expirationHours) {
        this.secret = secret;
        this.expirationHours = expirationHours;
    }

    public static final String ISSUED_AT_CLAIM = "issuedAt";

    public String genToken(Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        // 签发时间（秒）用于与 password_updated_time 比较，改密码后旧 token 即失效
        Map<String, Object> payload = new HashMap<>(claims);
        payload.put(ISSUED_AT_CLAIM, now / 1000);
        return JWT.create()
                .withClaim("claims", payload)
                .withExpiresAt(new Date(now + 1000 * 60 * 60 * expirationHours))
                .sign(Algorithm.HMAC256(secret));
    }

    public Map<String, Object> parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }
}
