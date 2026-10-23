package com.gestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/connexion", "/css/**", "/error").permitAll()

                        .requestMatchers("/offres/gestion", "/offres/nouvelle")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH")
                        .requestMatchers(HttpMethod.GET, "/offres/*/modifier")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH")
                        .requestMatchers("/offres/*/questions", "/offres/*/questions/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH")
                        .requestMatchers(HttpMethod.POST, "/offres", "/offres/*", "/offres/*/publier")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH")

                        .requestMatchers(HttpMethod.GET, "/demandes/nouvelle")
                        .hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMINISTRATEUR")
                        .requestMatchers(HttpMethod.POST, "/demandes")
                        .hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMINISTRATEUR")

                        .requestMatchers("/demandes/validation-rh", "/demandes/*/validation-rh/**")
                        .hasAuthority("ROLE_RH")
                        .requestMatchers("/demandes/validation/finance", "/demandes/*/validation/finance/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_FINANCE", "ROLE_MANAGER", "ROLE_EMPLOYE")
                        .requestMatchers("/demandes/validation/direction", "/demandes/*/validation/direction/**")
                        .hasAuthority("ROLE_ADMINISTRATEUR")

                        .requestMatchers("/candidatures/rh", "/candidatures/rh/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH")
                        .requestMatchers("/candidatures/postuler")
                        .hasAnyAuthority("ROLE_UTILISATEUR", "ROLE_EMPLOYE", "ROLE_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/candidatures")
                        .hasAnyAuthority("ROLE_UTILISATEUR", "ROLE_EMPLOYE", "ROLE_MANAGER")

                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/connexion")
                        .defaultSuccessUrl("/demandes")
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
