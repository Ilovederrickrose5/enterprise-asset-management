package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.entity.Department;
import com.enterprise.asset.enterpriseassetmanagement.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    
    @Autowired
    private DepartmentService departmentService;
    
    @GetMapping
    public ResponseEntity<Result<List<Department>>> getAllDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(Result.success(departments));
    }
    
    @GetMapping("/active")
    public ResponseEntity<Result<List<Department>>> getActiveDepartments() {
        List<Department> departments = departmentService.getActiveDepartments();
        return ResponseEntity.ok(Result.success(departments));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Result<Department>> getDepartmentById(@PathVariable Long id) {
        Department department = departmentService.getDepartmentById(id);
        if (department == null) {
            return ResponseEntity.ok(Result.error(404, "部门不存在"));
        }
        return ResponseEntity.ok(Result.success(department));
    }
    
    @GetMapping("/code/{deptCode}")
    public ResponseEntity<Result<Department>> getDepartmentByCode(@PathVariable String deptCode) {
        Department department = departmentService.getDepartmentByCode(deptCode);
        if (department == null) {
            return ResponseEntity.ok(Result.error(404, "部门不存在"));
        }
        return ResponseEntity.ok(Result.success(department));
    }
    
    @PostMapping
    public ResponseEntity<Result<Department>> createDepartment(@RequestBody Department department) {
        // 如果没有提供部门编码，自动生成
        if (department.getDeptCode() == null || department.getDeptCode().isEmpty()) {
            department.setDeptCode(departmentService.generateDeptCode());
        }
        Department createdDepartment = departmentService.createDepartment(department);
        return ResponseEntity.ok(Result.success(createdDepartment));
    }
    
    @GetMapping("/generate-code")
    public ResponseEntity<Result<String>> generateDeptCode() {
        String deptCode = departmentService.generateDeptCode();
        return ResponseEntity.ok(Result.success(deptCode));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Result<Department>> updateDepartment(
            @PathVariable Long id,
            @RequestBody Department department) {
        Department updatedDepartment = departmentService.updateDepartment(id, department);
        if (updatedDepartment == null) {
            return ResponseEntity.ok(Result.error(404, "部门不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedDepartment));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteDepartment(@PathVariable Long id) {
        boolean deleted = departmentService.deleteDepartment(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "部门不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }
    
    @GetMapping("/count")
    public ResponseEntity<Result<Long>> getActiveDepartmentCount() {
        long count = departmentService.getActiveDepartmentCount();
        return ResponseEntity.ok(Result.success(count));
    }
}
