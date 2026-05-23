package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.entity.Department;
import com.enterprise.asset.enterpriseassetmanagement.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public List<Department> getActiveDepartments() {
        return departmentRepository.findAll().stream()
                .filter(dept -> dept.getStatus() == 1)
                .toList();
    }

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id).orElse(null);
    }

    public Department getDepartmentByCode(String deptCode) {
        return departmentRepository.findByDeptCode(deptCode).orElse(null);
    }

    @Transactional
    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Transactional
    public Department updateDepartment(Long id, Department department) {
        Department existingDepartment = departmentRepository.findById(id).orElse(null);
        if (existingDepartment == null) {
            return null;
        }

        existingDepartment.setDeptName(department.getDeptName());
        existingDepartment.setDeptCode(department.getDeptCode());
        existingDepartment.setParentId(department.getParentId());
        existingDepartment.setLeader(department.getLeader());
        existingDepartment.setDescription(department.getDescription());
        existingDepartment.setStatus(department.getStatus());

        return departmentRepository.save(existingDepartment);
    }

    /**
     * 根据领导ID查询部门
     */
    public List<Department> getDepartmentsByLeader(Long leaderId) {
        return departmentRepository.findByLeader(leaderId);
    }

    @Transactional
    public boolean deleteDepartment(Long id) {
        if (departmentRepository.existsById(id)) {
            departmentRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public long getActiveDepartmentCount() {
        return departmentRepository.countByStatus(1);
    }

    /**
     * 生成部门编码
     * 规则：DEPT + 3位数字，从001开始递增
     */
    public String generateDeptCode() {
        // 获取所有部门编码
        List<Department> departments = departmentRepository.findAll();

        int maxCode = 0;
        for (Department dept : departments) {
            String deptCode = dept.getDeptCode();
            if (deptCode != null && deptCode.startsWith("DEPT")) {
                try {
                    String numPart = deptCode.substring(4);
                    int code = Integer.parseInt(numPart);
                    if (code > maxCode) {
                        maxCode = code;
                    }
                } catch (NumberFormatException e) {
                    // 忽略非数字编码
                }
            }
        }

        // 生成新编码
        return "DEPT" + String.format("%03d", maxCode + 1);
    }

    /**
     * 检查部门编码是否存在
     */
    public boolean isDeptCodeExists(String deptCode) {
        return departmentRepository.findByDeptCode(deptCode).isPresent();
    }
}
