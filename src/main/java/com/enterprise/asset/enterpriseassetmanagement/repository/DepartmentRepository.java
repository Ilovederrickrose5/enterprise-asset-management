package com.enterprise.asset.enterpriseassetmanagement.repository;

import com.enterprise.asset.enterpriseassetmanagement.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    long countByStatus(Integer status);
    
    Optional<Department> findByDeptCode(String deptCode);
    
    Optional<Department> findByDeptName(String deptName);
    
    List<Department> findByLeader(Long leader);
}
