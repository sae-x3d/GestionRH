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
                        .requestMatchers(HttpMethod.GET, "/demandes/nouvelle")
                        .hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMINISTRATEUR")
                        .requestMatchers(HttpMethod.POST, "/demandes")
                        .hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMINISTRATEUR")
                        .requestMatchers(HttpMethod.GET, "/demandes/validation-rh")
                        .hasAuthority("ROLE_RH")
                        .requestMatchers(HttpMethod.POST,
                                "/demandes/*/validation-rh/accepter",
                                "/demandes/*/validation-rh/refuser")
                        .hasAuthority("ROLE_RH")
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
