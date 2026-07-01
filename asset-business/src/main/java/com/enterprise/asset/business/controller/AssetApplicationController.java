package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.enums.ApplicationStatus;
import com.enterprise.asset.common.enums.ApplicationType;
import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.common.enums.UserRole;
import com.enterprise.asset.business.entity.AssetApplication;
import com.enterprise.asset.business.security.UserDetailsImpl;
import com.enterprise.asset.business.service.AssetApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** 资产业务申请控制器 - 处理领用/转移/维修/报废 */
@RestController
@RequestMapping("/api/asset-applications")
public class AssetApplicationController {

    private final AssetApplicationService assetApplicationService;

    public AssetApplicationController(AssetApplicationService assetApplicationService) {
        this.assetApplicationService = assetApplicationService;
    }

    // === 查询接口 ===
    @GetMapping
    public ResponseEntity<Result<Page<AssetApplication>>> getAllApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        Page<AssetApplication> applications;
        if ((type != null && !type.isEmpty()) || (status != null && !status.isEmpty())) {
            applications = assetApplicationService.getApplicationsByTypeAndStatus(type, status, page, size);
        } else {
            applications = assetApplicationService.getAllApplications(page, size);
        }
        return ResponseEntity.ok(Result.success(applications));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<AssetApplication>> getApplicationById(@PathVariable Long id) {
        AssetApplication application = assetApplicationService.getApplicationById(id);
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

    @GetMapping("/applicant/{applicantId}")
    public ResponseEntity<Result<Page<AssetApplication>>> getApplicationsByApplicantId(
            @PathVariable Long applicantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        Page<AssetApplication> applications;
        if ((type != null && !type.isEmpty()) || (status != null && !status.isEmpty())) {
            applications = assetApplicationService.getApplicationsByApplicantIdAndTypeAndStatus(applicantId, type,
                    status, page, size);
        } else {
            applications = assetApplicationService.getApplicationsByApplicantId(applicantId, page, size);
        }
        return ResponseEntity.ok(Result.success(applications));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsByStatus(@PathVariable String status) {
        List<AssetApplication> applications = assetApplicationService.getApplicationsByStatus(status);
        return ResponseEntity.ok(Result.success(applications));
    }

    @GetMapping("/status/{status}/department/{departmentId}")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsByStatusAndDepartment(
            @PathVariable String status, @PathVariable Long departmentId) {
        List<AssetApplication> applications = assetApplicationService.getApplicationsByStatusAndDepartment(status,
                departmentId);
        return ResponseEntity.ok(Result.success(applications));
    }

    @GetMapping("/status/pending_leader")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsPendingLeader() {
        List<AssetApplication> applications = assetApplicationService
                .getApplicationsByStatus(ApplicationStatus.PENDING_LEADER.getCode());
        return ResponseEntity.ok(Result.success(applications));
    }

    @GetMapping("/status/pending_leader/department/{departmentId}")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsPendingLeaderByDepartment(
            @PathVariable Long departmentId) {
        List<AssetApplication> applications = assetApplicationService
                .getApplicationsByStatusAndDepartment(ApplicationStatus.PENDING_LEADER.getCode(), departmentId);
        return ResponseEntity.ok(Result.success(applications));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Result<Page<AssetApplication>>> getApplicationsByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        Page<AssetApplication> applications;
        if ((type != null && !type.isEmpty()) || (status != null && !status.isEmpty())) {
            applications = assetApplicationService.getApplicationsByDepartmentIdAndTypeAndStatus(departmentId, type,
                    status, page, size);
        } else {
            applications = assetApplicationService.getApplicationsByDepartmentId(departmentId, page, size);
        }
        return ResponseEntity.ok(Result.success(applications));
    }

    // === 核心业务流程 ===

    /** 创建申请 - applicationType: RECEIVE/TRANSFER/MAINTENANCE/DISPOSAL */
    @PostMapping
    public ResponseEntity<Result<AssetApplication>> createApplication(@RequestBody AssetApplication application) {
        AssetApplication createdApplication = assetApplicationService.createApplication(application);
        return ResponseEntity.ok(Result.success(createdApplication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<AssetApplication>> updateApplication(
            @PathVariable Long id, @RequestBody AssetApplication application) {
        AssetApplication updatedApplication = assetApplicationService.updateApplication(id, application);
        if (updatedApplication == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedApplication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteApplication(@PathVariable Long id) {
        boolean deleted = assetApplicationService.deleteApplication(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    /** 批准申请 - 报废需二级审批(领导→资产管理员) */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Result<AssetApplication>> approveApplication(
            @PathVariable Long id, @RequestBody ApprovalRequest approvalRequest) {
        // 获取当前用户角色
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String userRole = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority()).findFirst().orElse("");

        AssetApplication existingApplication = assetApplicationService.getApplicationById(id);
        if (existingApplication == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }

        // 领导审批（一级）
        if (ApplicationType.DISPOSAL.getCode().equals(existingApplication.getApplicationType())) {
            if (UserRole.fromCode(userRole) != null
                    && (UserRole.fromCode(userRole).isAdmin() || UserRole.fromCode(userRole).isLeader())) {
                if (ApplicationStatus.PENDING_LEADER.getCode().equals(existingApplication.getStatus())) {
                    AssetApplication application = assetApplicationService.approveApplication(
                            id, approvalRequest.getApproverId(), approvalRequest.getApproverName(),
                            approvalRequest.getApprovalRemark());
                    if (existingApplication.getOriginalApplicationId() != null) {
                        assetApplicationService.updateStatus(existingApplication.getOriginalApplicationId(),
                                ApplicationStatus.LEADER_APPROVED.getCode());
                    }
                    return ResponseEntity.ok(Result.success(application));
                } else {
                    AssetApplication application = assetApplicationService.approveApplication(
                            id, approvalRequest.getApproverId(), approvalRequest.getApproverName(),
                            approvalRequest.getApprovalRemark());
                    return ResponseEntity.ok(Result.success(application));
                }
            } else {
                // 资产管理员审批（二级）
                if (ApplicationStatus.LEADER_APPROVED.getCode().equalsIgnoreCase(existingApplication.getStatus())) {
                    AssetApplication application = assetApplicationService.approveApplication(
                            id, approvalRequest.getApproverId(), approvalRequest.getApproverName(),
                            approvalRequest.getApprovalRemark());
                    return ResponseEntity.ok(Result.success(application));
                } else {
                    return ResponseEntity.ok(Result.error(400, "资产报废申请需要领导先批准"));
                }
            }
        } else {
            // 普通申请直接批准
            AssetApplication application = assetApplicationService.approveApplication(
                    id, approvalRequest.getApproverId(), approvalRequest.getApproverName(),
                    approvalRequest.getApprovalRemark());
            return ResponseEntity.ok(Result.success(application));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Result<AssetApplication>> rejectApplication(
            @PathVariable Long id, @RequestBody ApprovalRequest approvalRequest) {
        AssetApplication application = assetApplicationService.rejectApplication(
                id, approvalRequest.getApproverId(), approvalRequest.getApproverName(),
                approvalRequest.getApprovalRemark());
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

    // 部门资产管理员创建报废终审申请
    @PostMapping("/create-final-approval")
    public ResponseEntity<Result<AssetApplication>> createFinalApproval(
            @RequestBody AssetApplication finalApprovalRequest) {
        Long originalApplicationId = finalApprovalRequest.getOriginalApplicationId();
        finalApprovalRequest.setStatus(ApplicationStatus.PENDING_LEADER.getCode());
        finalApprovalRequest.setApplicationDate(LocalDateTime.now());
        AssetApplication finalApproval = assetApplicationService.createApplication(finalApprovalRequest);
        if (originalApplicationId != null) {
            assetApplicationService.updateStatus(originalApplicationId, "final_approval_created");
        }
        return ResponseEntity.ok(Result.success(finalApproval));
    }

    /** 开始维修 - 更新资产状态为维修中 */
    @PutMapping("/{id}/start-maintenance")
    public ResponseEntity<Result<AssetApplication>> startMaintenance(
            @PathVariable Long id, @RequestBody MaintenanceRequest request) {
        AssetApplication application = assetApplicationService.startMaintenance(id, request.getUserId(),
                request.getUserName());
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在或状态不允许"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

    /** 完成维修 - 更新资产状态为正常 */
    @PostMapping("/{id}/complete-maintenance")
    public ResponseEntity<Result<AssetApplication>> completeMaintenance(
            @PathVariable Long id, @RequestBody MaintenanceRequest request) {
        AssetApplication application = assetApplicationService.completeMaintenance(id, request.getUserId(),
                request.getUserName());
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在或状态不允许"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

    // === 请求参数类 ===
    static class MaintenanceRequest {
        private Long userId;
        private String userName;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }

    static class ApprovalRequest {
        private Long approverId;
        private String approverName;
        private String approvalRemark;

        public Long getApproverId() {
            return approverId;
        }

        public void setApproverId(Long approverId) {
            this.approverId = approverId;
        }

        public String getApproverName() {
            return approverName;
        }

        public void setApproverName(String approverName) {
            this.approverName = approverName;
        }

        public String getApprovalRemark() {
            return approvalRemark;
        }

        public void setApprovalRemark(String approvalRemark) {
            this.approvalRemark = approvalRemark;
        }
    }
}