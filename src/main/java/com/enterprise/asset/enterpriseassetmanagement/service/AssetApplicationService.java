package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.entity.AssetApplication;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AssetApplicationService {
    List<AssetApplication> getAllApplications();

    List<AssetApplication> getAllApplicationsByDepartment(Long departmentId);

    AssetApplication getApplicationById(Long id);

    AssetApplication createApplication(AssetApplication application);

    AssetApplication updateApplication(Long id, AssetApplication application);

    boolean deleteApplication(Long id);

    List<AssetApplication> getApplicationsByApplicantId(Long applicantId);

    List<AssetApplication> getApplicationsByStatus(String status);

    List<AssetApplication> getApplicationsByStatusAndDepartment(String status, Long departmentId);

    AssetApplication approveApplication(Long id, Long approverId, String approverName, String approvalRemark);

    AssetApplication approveApplicationForLeader(Long id, Long approverId, String approverName, String approvalRemark);

    AssetApplication rejectApplication(Long id, Long approverId, String approverName, String approvalRemark);

    AssetApplication updateStatus(Long id, String status);

    AssetApplication startMaintenance(Long id, Long userId, String userName);

    AssetApplication completeMaintenance(Long id, Long userId, String userName);
    
    // 分页查询方法
    Page<AssetApplication> getAllApplications(int page, int size);
    
    Page<AssetApplication> getApplicationsByApplicantId(Long applicantId, int page, int size);
    
    Page<AssetApplication> getApplicationsByDepartmentId(Long departmentId, int page, int size);
    
    Page<AssetApplication> getApplicationsByTypeAndStatus(String type, String status, int page, int size);
    
    Page<AssetApplication> getApplicationsByApplicantIdAndTypeAndStatus(Long applicantId, String type, String status, int page, int size);
    
    Page<AssetApplication> getApplicationsByDepartmentIdAndTypeAndStatus(Long departmentId, String type, String status, int page, int size);
}