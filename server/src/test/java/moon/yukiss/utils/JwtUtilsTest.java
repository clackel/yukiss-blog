package moon.yukiss.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {
    private final JwtUtils jwtUtils = new JwtUtils("test-secret", 12);

    @Test
    void tokenCarriesIssuedAtInSeconds() {
        long before = System.currentTimeMillis() / 1000;
        String token = jwtUtils.genToken(Map.of("id", 7, "username", "friend"));
        long after = System.currentTimeMillis() / 1000;

        Map<String, Object> claims = jwtUtils.parseToken(token);

        assertEquals(7, ((Number) claims.get("id")).intValue());
        assertEquals("friend", claims.get("username"));
        Number issuedAt = assertInstanceOf(Number.class, claims.get(JwtUtils.ISSUED_AT_CLAIM));
        assertTrue(issuedAt.longValue() >= before && issuedAt.longValue() <= after);
    }
}
