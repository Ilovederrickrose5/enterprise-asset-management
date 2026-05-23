package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.entity.Role;

import java.util.List;

public interface RoleService {
    
    List<Role> getAllRoles();
    
    Role getRoleById(Long id);
    
    Role getRoleByCode(String code);
    
    Role createRole(Role role);
    
    Role updateRole(Long id, Role role);
    
    boolean deleteRole(Long id);
}