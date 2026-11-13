package com.gestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final SuccesConnexionHandler succesConnexionHandler;
    private final SecurityVersionFilter securityVersionFilter;

    public SecurityConfig(SuccesConnexionHandler succesConnexionHandler, SecurityVersionFilter securityVersionFilter) {
        this.succesConnexionHandler = succesConnexionHandler;
        this.securityVersionFilter = securityVersionFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/connexion", "/mot-de-passe-oublie", "/mot-de-passe-reinitialiser",
                                "/css/**", "/error").permitAll()

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
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_DIRECTION")

                        .requestMatchers("/candidatures/rh", "/candidatures/rh/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH")

                        .requestMatchers("/conges/validation/rh", "/conges/validation/rh/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH")
                        .requestMatchers("/conges/validation/manager", "/conges/validation/manager/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH", "ROLE_MANAGER")
                        .requestMatchers("/conges/parametres", "/conges/parametres/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH")
                        .requestMatchers("/conges/batch", "/conges/batch/**")
                        .hasAuthority("ROLE_ADMINISTRATEUR")
                        .requestMatchers("/conges", "/conges/**")
                        .authenticated()

                        .requestMatchers("/validations/bloc4", "/validations/bloc4/**")
                        .authenticated()
                        .requestMatchers("/audit")
                        .hasAuthority("ROLE_ADMINISTRATEUR")
                        .requestMatchers("/sorties", "/sorties/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH")
                        .requestMatchers("/pointage/anomalies", "/pointage/anomalies/**",
                                "/pointage/equipe", "/pointage/equipe/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH", "ROLE_MANAGER")
                        .requestMatchers("/planning", "/planning/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH", "ROLE_MANAGER")
                        .requestMatchers("/pointage", "/pointage/**", "/heures-sup", "/heures-sup/**")
                        .authenticated()
                        .requestMatchers("/paie", "/paie/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH", "ROLE_FINANCE", "ROLE_PAIE")
                        .requestMatchers("/reporting", "/reporting/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH", "ROLE_FINANCE",
                                "ROLE_DIRECTION", "ROLE_PAIE")
                        .requestMatchers("/employes/changements", "/employes/changements/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH")
                        .requestMatchers("/employes", "/employes/**")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH", "ROLE_MANAGER",
                                "ROLE_EMPLOYE", "ROLE_DIRECTION", "ROLE_PAIE", "ROLE_FINANCE")
                        .requestMatchers("/accreditations", "/onboarding")
                        .hasAnyAuthority("ROLE_ADMINISTRATEUR", "ROLE_RH", "ROLE_MANAGER", "ROLE_IT")
                        .requestMatchers("/accreditations/**", "/mon-onboarding", "/onboarding/**")
                        .authenticated()
                        .requestMatchers("/candidatures/postuler")
                        .hasAnyAuthority("ROLE_UTILISATEUR", "ROLE_EMPLOYE", "ROLE_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/candidatures")
                        .hasAnyAuthority("ROLE_UTILISATEUR", "ROLE_EMPLOYE", "ROLE_MANAGER")

                        .requestMatchers(HttpMethod.GET, "/", "/offres", "/offres/*")
                        .permitAll()

                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/connexion")
                        .successHandler(succesConnexionHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/connexion?deconnexion")
                        .permitAll())
                .addFilterAfter(securityVersionFilter, SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
