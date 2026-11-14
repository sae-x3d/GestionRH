package com.sirh.backend.modules.auth.service;

import com.sirh.backend.modules.auth.dto.AuthSessionResponse;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.auth.repository.AuthUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AuthUserRepository users;

    public AuthService(AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository, AuthUserRepository users) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.users = users;
    }

    public AuthSessionResponse login(String email, String password,
            HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(email.trim(), password));
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return toResponse((AuthUserPrincipal) authentication.getPrincipal());
    }

    public AuthSessionResponse currentUser(AuthUserPrincipal principal) {
        return toResponse(users.findById(principal.getId()));
    }

    private AuthSessionResponse toResponse(AuthUserPrincipal principal) {
        return new AuthSessionResponse(principal.getId(), principal.getDisplayUsername(),
            principal.getEmail(), principal.getRoles(), principal.getAccountValidationStatus(), principal.getDepartmentCodes(),
            users.findPhotoPathById(principal.getId()) == null ? null : "/api/candidatures/me/photo?v=" + System.currentTimeMillis());
    }
}
