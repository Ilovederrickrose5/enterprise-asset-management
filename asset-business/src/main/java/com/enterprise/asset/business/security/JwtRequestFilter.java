package com.enterprise.asset.business.security;

import com.enterprise.asset.business.client.AuthFeignClient;
import com.enterprise.asset.common.dto.UserDTO;
import com.enterprise.asset.common.dto.ValidateTokenRequest;
import com.enterprise.asset.common.util.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 业务服务JWT过滤器
 * 所有token验证统一通过Feign调用auth服务的/api/auth/validate-token接口
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final AuthFeignClient authFeignClient;

    public JwtRequestFilter(AuthFeignClient authFeignClient) {
        this.authFeignClient = authFeignClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // 跳过认证相关接口（这些接口会被转发到auth服务）
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
        }

        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 通过Feign调用auth服务验证token
                ValidateTokenRequest validateRequest = new ValidateTokenRequest();
                validateRequest.setToken(jwt);

                Result<UserDTO> result = authFeignClient.validateToken(validateRequest);

                if (result != null && result.getCode() == 200 && result.getData() != null) {
                    UserDTO userDTO = result.getData();
                    logger.debug("Token validated successfully for user: {}", userDTO.getUsername());

                    // 基于UserDTO创建Authentication对象
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDTO,
                            null,
                            userDTO.getRoleCodes() != null ?
                                userDTO.getRoleCodes().stream()
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                    .collect(Collectors.toList()) :
                                Collections.emptyList()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Authentication set in SecurityContext for user: {}", userDTO.getUsername());
                } else {
                    logger.warn("Token validation failed: {}", result != null ? result.getMessage() : "No response");
                }
            } catch (Exception e) {
                logger.error("Error in JWT filter while calling auth service: {}", e.getMessage());
                // Feign调用失败时清除SecurityContext，避免使用失效的认证信息
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}