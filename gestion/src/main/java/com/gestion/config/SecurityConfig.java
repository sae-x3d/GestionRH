package com.gestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/connexion")
                        .defaultSuccessUrl("/demandes/nouvelle", true)
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
