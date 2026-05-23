package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.entity.Role;
import com.enterprise.asset.enterpriseassetmanagement.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping
    public ResponseEntity<Result<List<Role>>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(Result.success(roles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<Role>> getRoleById(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        if (role == null) {
            return ResponseEntity.ok(Result.error(404, "角色不存在"));
        }
        return ResponseEntity.ok(Result.success(role));
    }

    @PostMapping
    public ResponseEntity<Result<Role>> createRole(@RequestBody Role role) {
        Role createdRole = roleService.createRole(role);
        return ResponseEntity.ok(Result.success(createdRole));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<Role>> updateRole(
            @PathVariable Long id,
            @RequestBody Role role) {
        Role updatedRole = roleService.updateRole(id, role);
        if (updatedRole == null) {
            return ResponseEntity.ok(Result.error(404, "角色不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedRole));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteRole(@PathVariable Long id) {
        boolean deleted = roleService.deleteRole(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "角色不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }
}