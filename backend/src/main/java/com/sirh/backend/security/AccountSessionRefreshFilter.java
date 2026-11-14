package com.sirh.backend.security;

import java.io.IOException;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.auth.repository.AuthUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

public class AccountSessionRefreshFilter extends OncePerRequestFilter {
    private final AuthUserRepository users;
    private final SecurityContextRepository contexts;

    public AccountSessionRefreshFilter(AuthUserRepository users, SecurityContextRepository contexts) {
        this.users = users;
        this.contexts = contexts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthUserPrincipal principal) {
            AuthUserPrincipal current;
            try {
                current = users.findById(principal.getId());
            } catch (RuntimeException exception) {
                expire(request, response);
                return;
            }
            if (!current.isEnabled() || "REJECTED".equalsIgnoreCase(current.getAccountValidationStatus())) {
                expire(request, response);
                return;
            }
            if (!current.getRoles().equals(principal.getRoles())
                    || !current.getDepartmentCodes().equals(principal.getDepartmentCodes())
                    || current.isAccountNonLocked() != principal.isAccountNonLocked()) {
                SecurityContext context = SecurityContextHolder.getContext();
                var refreshed = UsernamePasswordAuthenticationToken.authenticated(current,
                    authentication.getCredentials(), current.getAuthorities());
                refreshed.setDetails(authentication.getDetails());
                context.setAuthentication(refreshed);
                contexts.saveContext(context, request, response);
            }
        }
        chain.doFilter(request, response);
    }

    private void expire(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Le compte n’est plus actif. Reconnectez-vous ou contactez le gestionnaire.");
    }
}
