package com.enterprise.asset.enterpriseassetmanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // 跳过登录和认证相关接口
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        System.out.println("=== JWT Filter Debug ===");
        System.out.println("Request URI: " + request.getRequestURI());
        System.out.println("Authorization Header: " + authorizationHeader);

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            System.out.println("JWT Token: " + jwt.substring(0, Math.min(50, jwt.length())) + "...");
            try {
                username = jwtUtil.extractUsername(jwt);
                System.out.println("Extracted Username: " + username);
            } catch (Exception e) {
                System.err.println("JWT Token extraction failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No valid Authorization header found");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                System.out.println("UserDetails loaded: " + userDetails.getUsername());
                System.out.println("UserDetails authorities: " + userDetails.getAuthorities());

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    System.out.println("Token validated successfully");

                    // 从Token中提取角色信息
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    try {
                        Map<String, Object> claims = jwtUtil.extractAllClaims(jwt);
                        Object rolesObj = claims.get("roles");
                        if (rolesObj instanceof List<?>) {
                            List<?> rolesList = (List<?>) rolesObj;
                            for (Object roleObj : rolesList) {
                                if (roleObj instanceof String) {
                                    authorities.add(new SimpleGrantedAuthority((String) roleObj));
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to extract roles from JWT: " + e.getMessage());
                        e.printStackTrace();
                    }

                    // 如果没有从Token中提取到角色，使用UserDetails中的角色
                    if (authorities.isEmpty()) {
                        System.out.println("Using authorities from UserDetails");
                        authorities = new ArrayList<>(userDetails.getAuthorities());
                    }

                    System.out.println("Final authorities: " + authorities);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("Authentication set in SecurityContext");
                } else {
                    System.out.println("Token validation failed");
                }
            } catch (Exception e) {
                System.err.println("Error in JWT filter: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Username is null or authentication already exists");
            System.out.println("Current Authentication: " + SecurityContextHolder.getContext().getAuthentication());
        }

        System.out.println("=== End JWT Filter Debug ===");
        chain.doFilter(request, response);
    }
}
