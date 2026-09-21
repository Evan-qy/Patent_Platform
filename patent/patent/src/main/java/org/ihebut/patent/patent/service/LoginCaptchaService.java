package org.ihebut.patent.patent.service;

import org.ihebut.patent.patent.dto.LoginCaptchaResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class LoginCaptchaService {
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();

    public LoginCaptchaResponse createCaptcha() {
        cleanupExpired();
        int left = random.nextInt(8) + 1;
        int right = random.nextInt(8) + 1;
        String captchaId = UUID.randomUUID().toString();
        captchaStore.put(captchaId, new CaptchaEntry(String.valueOf(left + right), Instant.now().plus(CAPTCHA_TTL)));
        return new LoginCaptchaResponse(captchaId, left + " + " + right + " = ?", CAPTCHA_TTL.toSeconds());
    }

    public void verify(String captchaId, String captchaAnswer) {
        cleanupExpired();
        if (captchaId == null || captchaId.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "验证码标识不能为空");
        }
        if (captchaAnswer == null || captchaAnswer.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "验证码不能为空");
        }
        CaptchaEntry entry = captchaStore.remove(captchaId.trim());
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "验证码已失效，请刷新后重试");
        }
        if (!entry.answer().equals(captchaAnswer.trim())) {
            throw new ResponseStatusException(BAD_REQUEST, "验证码错误");
        }
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, CaptchaEntry>> iterator = captchaStore.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt().isBefore(now)) {
                iterator.remove();
            }
        }
    }

    private record CaptchaEntry(String answer, Instant expiresAt) {
    }
}
