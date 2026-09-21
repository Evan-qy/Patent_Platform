package org.ihebut.patent.patent.security;

public record AppAuthPrincipal(Long userId, String username, TokenScope scope) {
    public boolean isAdmin() {
        return scope == TokenScope.ADMIN;
    }

    public enum TokenScope {
        USER,
        ADMIN
    }
}
