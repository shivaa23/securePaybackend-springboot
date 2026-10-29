package com.securepay.common.security;

import com.securepay.modules.user.entity.User;

import java.security.Principal;

/**
 * Authenticated principal wrapping the resolved {@link User}.
 * Replaces {@code Auth::user()} — inject via {@code @AuthenticationPrincipal CurrentUser} (doc 07 §7.1).
 */
public record CurrentUser(User user) implements Principal {

    @Override
    public String getName() {
        return user != null ? user.getName() : null;
    }

    public Long id() {
        return user != null ? user.getId() : null;
    }

    public String role() {
        return user != null ? user.getRole() : null;
    }
}
