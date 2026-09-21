package org.ihebut.patent.patent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.LoginCaptchaResponse;
import org.ihebut.patent.patent.dto.LoginRequest;
import org.ihebut.patent.patent.dto.RegisterRequest;
import org.ihebut.patent.patent.entity.UserAccount;
import org.ihebut.patent.patent.entity.UserProfile;
import org.ihebut.patent.patent.mapper.UserAccountMapper;
import org.ihebut.patent.patent.mapper.UserProfileMapper;
import org.ihebut.patent.patent.security.JwtService;
import org.ihebut.patent.patent.service.AuditLogService;
import org.ihebut.patent.patent.service.LoginCaptchaService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountMapper userAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginCaptchaService loginCaptchaService;
    private final AuditLogService auditLogService;

    public AuthController(
            UserAccountMapper userAccountMapper,
            UserProfileMapper userProfileMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginCaptchaService loginCaptchaService,
            AuditLogService auditLogService
    ) {
        this.userAccountMapper = userAccountMapper;
        this.userProfileMapper = userProfileMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginCaptchaService = loginCaptchaService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/captcha")
    public ApiResponse<LoginCaptchaResponse> captcha() {
        return ApiResponse.ok(loginCaptchaService.createCaptcha());
    }

    @PostMapping("/register")
    @Transactional
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "密码不能为空");
        }
        if (userAccountMapper.findByUsername(request.getUsername().trim()).isPresent()) {
            auditLogService.logAnonymousAction(httpRequest, request.getUsername().trim(), "REGISTER", "用户注册", "用户名已存在", false);
            throw new ResponseStatusException(BAD_REQUEST, "用户名已存在");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user = userAccountMapper.save(user);

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            UserProfile profile = new UserProfile();
            profile.setUser(user);
            profile.setNickname(request.getNickname().trim());
            userProfileMapper.save(profile);
        }

        auditLogService.logUserAction(
                httpRequest,
                user.getId(),
                "REGISTER",
                "用户注册",
                "user_account",
                String.valueOf(user.getId()),
                "注册新账号: " + user.getUsername(),
                true
        );

        return ApiResponse.ok(Map.of("userId", user.getId()));
    }

    @PostMapping("/login")
    public ApiResponse<String> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "账号不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "密码不能为空");
        }

        String identifier = request.getUsername().trim();
        loginCaptchaService.verify(request.getCaptchaId(), request.getCaptchaAnswer());

        UserAccount user = userAccountMapper.findByUsername(identifier)
                .or(() -> userAccountMapper.findByEmail(identifier))
                .or(() -> userAccountMapper.findByPhone(identifier))
                .orElseThrow(() -> {
                    auditLogService.logAnonymousAction(httpRequest, identifier, "LOGIN", "用户登录", "账号不存在或密码错误", false);
                    return new ResponseStatusException(BAD_REQUEST, "账号或密码错误");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditLogService.logUserAction(
                    httpRequest,
                    user.getId(),
                    "LOGIN",
                    "用户登录",
                    "user_account",
                    String.valueOf(user.getId()),
                    "密码校验失败",
                    false
            );
            throw new ResponseStatusException(BAD_REQUEST, "账号或密码错误");
        }

        auditLogService.logUserAction(
                httpRequest,
                user.getId(),
                "LOGIN",
                "用户登录",
                "user_account",
                String.valueOf(user.getId()),
                "登录成功",
                true
        );

        return ApiResponse.ok(jwtService.createToken(user.getId()));
    }
}
