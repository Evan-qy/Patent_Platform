package org.ihebut.patent.patent.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
public class CurrentUser {
    public long requireUserId() {
        AppAuthPrincipal principal = getPrincipal();
        if (principal == null || principal.userId() == null || principal.isAdmin()) {
            throw new ResponseStatusException(UNAUTHORIZED, "未登录");
        }
        return principal.userId();
    }

    public AppAuthPrincipal requireAdmin() {
        AppAuthPrincipal principal = getPrincipal();
        if (principal == null || !principal.isAdmin()) {
            throw new ResponseStatusException(UNAUTHORIZED, "未登录后台");
        }
        return principal;
    }

    private AppAuthPrincipal getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof AppAuthPrincipal appAuthPrincipal) {
            return appAuthPrincipal;
        }
        if (principal instanceof Long id) {
            return new AppAuthPrincipal(id, String.valueOf(id), AppAuthPrincipal.TokenScope.USER);
        }
        if (principal instanceof String value) {
            try {
                return new AppAuthPrincipal(Long.parseLong(value), value, AppAuthPrincipal.TokenScope.USER);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
