package org.ihebut.patent.patent.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api",
            "/api/",
            "/api/auth/**",
            "/api/admin/auth/**",
            "/error"
    };

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    @Profile({"local", "test"})
    public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        return configure(http);
    }

    @Bean
    @Profile("!local & !test")
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        return configure(http);
    }

    private SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.httpBasic(basic -> basic.disable());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/ai/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/home-content").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/articles", "/api/articles/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/patents/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/requirements", "/api/requirements/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/patent-categories", "/api/patent-categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/patent-datasets", "/api/patent-datasets/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/experts/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/organizations/**").permitAll()
                .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
