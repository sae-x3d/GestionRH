package com.gestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/connexion", "/css/**", "/error").permitAll()
                        .requestMatchers("/offres/nouvelle", "/offres/gestion").hasAnyRole("ADMINISTRATEUR", "RH")
                        .requestMatchers("/offres/*/modifier").hasAnyRole("ADMINISTRATEUR", "RH")
                        .requestMatchers("/offres/*/questions", "/offres/*/questions/**")
                        .hasAnyRole("ADMINISTRATEUR", "RH")
                        .requestMatchers(HttpMethod.POST, "/offres", "/offres/*", "/offres/*/publier")
                        .hasAnyRole("ADMINISTRATEUR", "RH")
                        .requestMatchers("/demandes/nouvelle").hasAnyRole("ADMINISTRATEUR", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/demandes").hasAnyRole("ADMINISTRATEUR", "MANAGER")
                        .requestMatchers("/demandes/validation/rh", "/demandes/*/validation/rh/**")
                        .hasAnyRole("ADMINISTRATEUR", "RH")
                        .requestMatchers("/demandes/validation/finance", "/demandes/*/validation/finance/**")
                        .authenticated()
                        .requestMatchers("/demandes/validation/direction", "/demandes/*/validation/direction/**")
                        .hasRole("ADMINISTRATEUR")
                        .requestMatchers("/candidatures/rh", "/candidatures/rh/**")
                        .hasAnyRole("ADMINISTRATEUR", "RH")
                        .requestMatchers("/candidatures/postuler").hasAnyRole("UTILISATEUR", "EMPLOYE", "MANAGER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/connexion")
                        .successHandler((request, response, authentication) -> {
                            boolean utilisateurSimple = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> "ROLE_UTILISATEUR".equals(authority.getAuthority()));
                            boolean managerOuAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> "ROLE_ADMINISTRATEUR".equals(authority.getAuthority())
                                            || "ROLE_MANAGER".equals(authority.getAuthority()));
                            if (utilisateurSimple) {
                                response.sendRedirect("/offres");
                            } else if (managerOuAdmin) {
                                response.sendRedirect("/demandes/nouvelle");
                            } else {
                                response.sendRedirect("/offres");
                            }
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/connexion?deconnexion")
                        .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
