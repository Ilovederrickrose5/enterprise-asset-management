package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.entity.User;
import com.enterprise.asset.enterpriseassetmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<Result<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(Result.success(users));
    }

    @GetMapping("/active")
    public ResponseEntity<Result<List<User>>> getActiveUsers() {
        List<User> users = userService.getActiveUsers();
        return ResponseEntity.ok(Result.success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<User>> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(Result.success(user));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<Result<User>> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(Result.success(user));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Result<List<User>>> getUsersByDepartment(@PathVariable Long departmentId) {
        List<User> users = userService.getUsersByDepartment(departmentId);
        return ResponseEntity.ok(Result.success(users));
    }

    @PostMapping
    public ResponseEntity<Result<User>> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(Result.success(createdUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<User>> updateUser(
            @PathVariable Long id,
            @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        if (updatedUser == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<Result<User>> updateRole(
            @PathVariable Long userId,
            @RequestBody List<String> roleCodes) {
        // 如果是数组且有值，取第一个角色（当前系统每个用户只有一个角色）
        String roleCode = roleCodes != null && !roleCodes.isEmpty() ? roleCodes.get(0) : null;
        User user = userService.updateRole(userId, roleCode);
        if (user == null) {
            return ResponseEntity.ok(Result.error(404, "用户不存在"));
        }
        return ResponseEntity.ok(Result.success(user));
    }

    @GetMapping("/count")
    public ResponseEntity<Result<Long>> getActiveUserCount() {
        long count = userService.getActiveUserCount();
        return ResponseEntity.ok(Result.success(count));
    }

    @GetMapping("/count/department/{departmentId}")
    public ResponseEntity<Result<Long>> getUserCountByDepartment(@PathVariable Long departmentId) {
        long count = userService.getUserCountByDepartment(departmentId);
        return ResponseEntity.ok(Result.success(count));
    }
}
