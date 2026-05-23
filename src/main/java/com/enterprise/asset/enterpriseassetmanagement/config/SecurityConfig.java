package com.enterprise.asset.enterpriseassetmanagement.config;

import com.enterprise.asset.enterpriseassetmanagement.security.JwtRequestFilter;
import com.enterprise.asset.enterpriseassetmanagement.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    // 使用明文密码（不加密）- 仅用于开发和测试环境
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return rawPassword.toString();
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return rawPassword.toString().equals(encodedPassword);
            }
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许所有来源，但配合allowCredentials使用时需要指定具体来源
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("admin")
                        .requestMatchers("/api/leader/**").hasAnyRole("leader", "admin")
                        .requestMatchers("/api/manager/**").hasAnyRole("manager", "leader", "admin")
                        .requestMatchers("/api/inbound/**").hasAnyRole("manager", "admin")
                        .requestMatchers("/api/departments/**").hasAnyRole("user", "manager", "leader", "admin")
                        .requestMatchers("/api/suppliers/**").hasAnyRole("user", "manager", "leader", "admin")
                        .requestMatchers("/api/roles/**").hasAnyRole("manager", "leader", "admin")
                        .requestMatchers("/api/asset-applications/**").hasAnyRole("user", "manager", "leader", "admin")
                        .requestMatchers("/api/assets/**").hasAnyRole("user", "manager", "leader", "admin")
                        .requestMatchers("/api/asset-categories/**").hasAnyRole("user", "manager", "leader", "admin")
                        .requestMatchers("/api/purchase-orders/**").hasAnyRole("manager", "leader", "admin")
                        .requestMatchers("/api/purchase-requests/pending/**").hasAnyRole("leader", "admin")
                        .requestMatchers("/api/purchase-requests/approved/**").hasAnyRole("leader", "admin")
                        .requestMatchers("/api/purchase-requests/*/approve").hasAnyRole("leader", "admin")
                        .requestMatchers("/api/purchase-requests/*/reject").hasAnyRole("leader", "admin")
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
