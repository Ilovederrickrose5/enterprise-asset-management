package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.entity.Role;
import com.enterprise.asset.enterpriseassetmanagement.entity.User;

import com.enterprise.asset.enterpriseassetmanagement.repository.RoleRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getActiveUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getStatus() == 1)
                .toList();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public List<User> getUsersByDepartment(Long departmentId) {
        return userRepository.findAll().stream()
                .filter(user -> user.getDeptId() != null && user.getDeptId().equals(departmentId))
                .toList();
    }

    @Transactional
    public User createUser(User user) {
        // 检查用户名是否已存在
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在（如果提供了邮箱）
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new RuntimeException("邮箱已被使用");
            }
        }

        // 检查手机号是否已存在（如果提供了手机号）
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            if (userRepository.findByPhone(user.getPhone()).isPresent()) {
                throw new RuntimeException("手机号已被使用");
            }
        }

        // 如果角色为空，默认设置为普通用户
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("user");
        } else {
            // 将角色转换为小写
            user.setRole(user.getRole().toLowerCase());
        }

        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User user) {
        User existingUser = userRepository.findById(id).orElse(null);
        if (existingUser == null) {
            return null;
        }

        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setRealName(user.getRealName());
        existingUser.setEmployeeNo(user.getEmployeeNo());
        existingUser.setDeptId(user.getDeptId());
        existingUser.setPosition(user.getPosition());
        existingUser.setStatus(user.getStatus());
        // 将角色转换为小写
        if (user.getRole() != null) {
            existingUser.setRole(user.getRole().toLowerCase());
        }

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(existingUser);
    }

    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public User updateRole(Long userId, String roleIdStr) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        // 如果传入的是角色ID（数字），转换为角色代码
        String roleCode = roleIdStr;
        try {
            Long roleId = Long.parseLong(roleIdStr);
            Role role = roleRepository.findById(roleId).orElse(null);
            if (role != null) {
                roleCode = role.getCode();
            }
        } catch (NumberFormatException e) {
            // 如果不是数字，直接使用作为角色代码
        }

        // 将角色代码转换为小写
        if (roleCode != null) {
            roleCode = roleCode.toLowerCase();
        }
        user.setRole(roleCode);
        return userRepository.save(user);
    }

    public long getActiveUserCount() {
        return userRepository.countActiveUsers();
    }

    public long getUserCountByDepartment(Long departmentId) {
        return userRepository.countByDeptId(departmentId);
    }
}
