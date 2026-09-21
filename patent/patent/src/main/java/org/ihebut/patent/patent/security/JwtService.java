package org.ihebut.patent.patent.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String CLAIM_SCOPE = "scope";

    private final SecretKey userKey;
    private final SecretKey adminKey;
    private final long userExpirationMs;
    private final long adminExpirationMs;

    public JwtService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expirationMs:600000}") long expirationMs,
            @Value("${admin.jwt.secret:}") String adminSecret,
            @Value("${admin.jwt.expiration-ms:600000}") long adminExpirationMs
    ) {
        this.userExpirationMs = expirationMs;
        this.adminExpirationMs = adminExpirationMs;
        this.userKey = buildKey(secret);
        this.adminKey = buildKey(adminSecret == null || adminSecret.isBlank() ? deriveAdminSecret(secret) : adminSecret);
        if (isBlank(secret) || isBlank(adminSecret)) {
            log.warn("未配置 jwt.secret / admin.jwt.secret，已为缺失项生成随机密钥："
                    + "重启后旧 Token 全部失效。生产环境请通过环境变量注入固定密钥。");
        }
    }

    public String createToken(Long userId) {
        return createUserToken(userId);
    }

    public String createUserToken(Long userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_SCOPE, AppAuthPrincipal.TokenScope.USER.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + userExpirationMs))
                .signWith(userKey)
                .compact();
    }

    public String createAdminToken(String username) {
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_SCOPE, AppAuthPrincipal.TokenScope.ADMIN.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + adminExpirationMs))
                .signWith(adminKey)
                .compact();
    }

    public Long parseToken(String token) {
        AppAuthPrincipal principal = parseAuthPrincipal(token);
        if (principal == null || principal.scope() != AppAuthPrincipal.TokenScope.USER) {
            return null;
        }
        return principal.userId();
    }

    public AppAuthPrincipal parseAuthPrincipal(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(userKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return toPrincipal(claims, AppAuthPrincipal.TokenScope.USER);
        } catch (Exception ignored) {
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(adminKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return toPrincipal(claims, AppAuthPrincipal.TokenScope.ADMIN);
        } catch (Exception ignored) {
            return null;
        }
    }

    private AppAuthPrincipal toPrincipal(Claims claims, AppAuthPrincipal.TokenScope fallbackScope) {
        String scopeValue = claims.get(CLAIM_SCOPE, String.class);
        AppAuthPrincipal.TokenScope scope = scopeValue == null || scopeValue.isBlank()
                ? fallbackScope
                : AppAuthPrincipal.TokenScope.valueOf(scopeValue);
        if (scope == AppAuthPrincipal.TokenScope.ADMIN) {
            return new AppAuthPrincipal(null, claims.getSubject(), scope);
        }
        return new AppAuthPrincipal(Long.parseLong(claims.getSubject()), claims.getSubject(), scope);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 由用户端密钥派生管理端密钥。基础密钥缺失时必须回落为空串，
     * 否则空密钥会被补零成 "-admin000..." 这类可预测密钥，导致管理员 Token 可被伪造。
     */
    private static String deriveAdminSecret(String secret) {
        return isBlank(secret) ? "" : secret + "-admin";
    }

    private static SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            return Jwts.SIG.HS256.key().build();
        }
        return Keys.hmacShaKeyFor(normalizeSecret(secret).getBytes(StandardCharsets.UTF_8));
    }

    private static String normalizeSecret(String secret) {
        String s = secret.trim();
        if (s.length() >= 32) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < 32) sb.append('0');
        return sb.toString();
    }
}
