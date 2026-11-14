package com.sirh.backend.modules.auth.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthUserPrincipal implements UserDetails, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Integer id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean suspended;
    private final String accountValidationStatus;
    private final List<String> roles;
    private final List<String> departmentCodes;

    public AuthUserPrincipal(Integer id, String username, String email, String passwordHash,
            boolean suspended, String accountValidationStatus, List<String> roles) {
        this(id, username, email, passwordHash, suspended, accountValidationStatus, roles, List.of());
    }

    public AuthUserPrincipal(Integer id, String username, String email, String passwordHash,
            boolean suspended, String accountValidationStatus, List<String> roles, List<String> departmentCodes) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.suspended = suspended;
        this.accountValidationStatus = accountValidationStatus;
        this.roles = List.copyOf(roles);
        this.departmentCodes = List.copyOf(departmentCodes);
    }

    public Integer getId() { return id; }
    public String getDisplayUsername() { return username; }
    public String getEmail() { return email; }
    public String getAccountValidationStatus() { return accountValidationStatus; }
    public List<String> getRoles() { return roles; }
    public List<String> getDepartmentCodes() { return departmentCodes; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
            .toList();
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return !suspended; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return !suspended; }
}
