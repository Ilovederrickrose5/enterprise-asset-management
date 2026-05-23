package com.enterprise.asset.enterpriseassetmanagement.security;

import com.enterprise.asset.enterpriseassetmanagement.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
public class UserDetailsImpl implements UserDetails {

    public UserDetailsImpl(Long id, String username, String email, String password, String realName, Long departmentId,
            String position, Integer status, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.realName = realName;
        this.departmentId = departmentId;
        this.position = position;
        this.status = status;
        this.authorities = authorities;
    }

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;

    @JsonIgnore
    private String password;

    private String realName;
    private Long departmentId;
    private String position;
    private Integer status;

    private Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        // 如果角色为空，默认设置为普通用户角色
        String role = user.getRole() != null ? user.getRole().toLowerCase() : "user";
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRealName(),
                user.getDeptId(),
                user.getPosition(),
                user.getStatus(),
                authorities);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
