package com.enterprise.asset.auth.util;

import com.enterprise.asset.auth.entity.User;

import java.util.List;

/**
 * 角色检查工具类
 * 提供用户角色判断的便捷方法
 */
public class RoleUtils {

    /** 管理员角色编码 */
    public static final String ROLE_ADMIN = "ADMIN";
    
    /** 领导角色编码 */
    public static final String ROLE_LEADER = "LEADER";
    
    /** 资产管理员角色编码 */
    public static final String ROLE_MANAGER = "MANAGER";
    
    /** 普通用户角色编码 */
    public static final String ROLE_USER = "USER";

    /**
     * 检查用户是否为管理员
     */
    public static boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        return hasRole(user, ROLE_ADMIN);
    }

    /**
     * 检查用户是否为领导
     */
    public static boolean isLeader(User user) {
        if (user == null) {
            return false;
        }
        return hasRole(user, ROLE_LEADER);
    }

    /**
     * 检查用户是否为资产管理员
     */
    public static boolean isManager(User user) {
        if (user == null) {
            return false;
        }
        return hasRole(user, ROLE_MANAGER);
    }

    /**
     * 检查用户是否为普通用户
     */
    public static boolean isUser(User user) {
        if (user == null) {
            return false;
        }
        return hasRole(user, ROLE_USER);
    }

    /**
     * 检查用户是否拥有指定角色（不区分大小写）
     */
    public static boolean hasRole(User user, String roleCode) {
        if (user == null || roleCode == null) {
            return false;
        }
        List<String> roleCodes = user.getRoleCodes();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        String upperRoleCode = roleCode.toUpperCase();
        return roleCodes.stream()
                .anyMatch(code -> upperRoleCode.equals(code.toUpperCase()));
    }

    /**
     * 检查用户是否拥有任意一个指定角色
     */
    public static boolean hasAnyRole(User user, String... roleCodes) {
        if (user == null || roleCodes == null || roleCodes.length == 0) {
            return false;
        }
        List<String> userRoles = user.getRoleCodes();
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        for (String roleCode : roleCodes) {
            if (hasRole(user, roleCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查用户是否拥有所有指定角色
     */
    public static boolean hasAllRoles(User user, String... roleCodes) {
        if (user == null || roleCodes == null || roleCodes.length == 0) {
            return false;
        }
        List<String> userRoles = user.getRoleCodes();
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        for (String roleCode : roleCodes) {
            if (!hasRole(user, roleCode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取用户角色编码（兼容旧代码）
     * 返回第一个角色，如果没有角色返回null
     */
    public static String getRoleCode(User user) {
        if (user == null) {
            return null;
        }
        List<String> roleCodes = user.getRoleCodes();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return null;
        }
        return roleCodes.get(0);
    }
}