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
                        .requestMatchers(HttpMethod.POST, "/offres", "/offres/*", "/offres/*/publier")
                        .hasAnyRole("ADMINISTRATEUR", "RH")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/connexion")
                        .successHandler((request, response, authentication) -> {
                            boolean utilisateurSimple = authentication.getAuthorities().stream()
                                    .anyMatch(authority -> "ROLE_UTILISATEUR".equals(authority.getAuthority()));
                            response.sendRedirect(utilisateurSimple ? "/offres" : "/demandes/nouvelle");
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
