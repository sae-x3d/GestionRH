package com.sirh.backend.modules.auth.controller;

import com.sirh.backend.modules.auth.dto.AuthSessionResponse;
import com.sirh.backend.modules.auth.dto.CsrfResponse;
import com.sirh.backend.modules.auth.dto.LoginRequest;
import com.sirh.backend.modules.auth.dto.RegistrationRequest;
import com.sirh.backend.modules.auth.dto.RegistrationResponse;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.auth.service.AuthService;
import com.sirh.backend.modules.auth.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final RegistrationService registrationService;
    private final CookieCsrfTokenRepository csrfTokenRepository;

    public AuthController(AuthService authService, RegistrationService registrationService,
            CookieCsrfTokenRepository csrfTokenRepository) {
        this.authService = authService;
        this.registrationService = registrationService;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken());
    }

    @PostMapping("/login")
    public AuthSessionResponse login(@Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request, HttpServletResponse response) {
        return authService.login(loginRequest.email(), loginRequest.password(), request, response);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        return registrationService.register(registrationRequest);
    }

    @GetMapping("/me")
    public AuthSessionResponse currentUser(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return principal == null ? null : authService.currentUser(principal);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        csrfTokenRepository.saveToken(null, request, response);
    }
}
