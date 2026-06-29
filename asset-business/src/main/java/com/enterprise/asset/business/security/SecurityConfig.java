package com.enterprise.asset.business.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 业务服务安全配置
 * 所有认证逻辑通过Feign调用auth服务完成
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
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
                        // 认证相关接口转发到auth服务，不需要在business服务中验证
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 业务接口权限配置
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/leader/**").hasAnyRole("LEADER", "ADMIN")
                        .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/inbound/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/departments/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/suppliers/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/roles/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/asset-applications/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/assets/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/asset-categories/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/asset-inventory/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/depreciation/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/depreciation-report/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/disposal-records/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/purchase-orders/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/purchase-requests/**").hasAnyRole("MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/reports/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .requestMatchers("/api/dashboard/**").hasAnyRole("USER", "MANAGER", "LEADER", "ADMIN")
                        .anyRequest().authenticated());

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}