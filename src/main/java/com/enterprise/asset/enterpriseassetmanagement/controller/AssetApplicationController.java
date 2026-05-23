package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.entity.AssetApplication;
import com.enterprise.asset.enterpriseassetmanagement.security.UserDetailsImpl;
import com.enterprise.asset.enterpriseassetmanagement.service.AssetApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/asset-applications")
public class AssetApplicationController {

    @Autowired
    private AssetApplicationService assetApplicationService;

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
        List<AssetApplication> applications = assetApplicationService.getApplicationsByStatus("pending_leader");
        return ResponseEntity.ok(Result.success(applications));
    }

    @GetMapping("/status/pending_leader/department/{departmentId}")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsPendingLeaderByDepartment(
            @PathVariable Long departmentId) {
        List<AssetApplication> applications = assetApplicationService
                .getApplicationsByStatusAndDepartment("pending_leader", departmentId);
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

    @PostMapping
    public ResponseEntity<Result<AssetApplication>> createApplication(@RequestBody AssetApplication application) {
        AssetApplication createdApplication = assetApplicationService.createApplication(application);
        return ResponseEntity.ok(Result.success(createdApplication));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<AssetApplication>> updateApplication(
            @PathVariable Long id,
            @RequestBody AssetApplication application) {
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

    @PostMapping("/{id}/approve")
    public ResponseEntity<Result<AssetApplication>> approveApplication(
            @PathVariable Long id,
            @RequestBody ApprovalRequest approvalRequest) {
        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String userRole = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .findFirst()
                .orElse("");

        // 检查申请类型
        AssetApplication existingApplication = assetApplicationService.getApplicationById(id);
        if (existingApplication == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }

        // 检查是否为报废申请
        if ("DISPOSAL".equals(existingApplication.getApplicationType())) {
            // 领导或系统管理员批准
            if ("ROLE_admin".equals(userRole) || "ROLE_leader".equals(userRole)) {
                // 如果这是一个终审申请（状态为pending_leader），需要更新原申请状态
                if ("pending_leader".equals(existingApplication.getStatus())) {
                    // 批准终审申请
                    AssetApplication application = assetApplicationService.approveApplication(
                            id,
                            approvalRequest.getApproverId(),
                            approvalRequest.getApproverName(),
                            approvalRequest.getApprovalRemark());

                    // 更新原申请状态为领导已批准（如果有原申请ID）
                    if (existingApplication.getOriginalApplicationId() != null) {
                        assetApplicationService.updateStatus(
                                existingApplication.getOriginalApplicationId(),
                                "leader_approved");
                    }

                    if (application == null) {
                        return ResponseEntity.ok(Result.error(404, "申请不存在"));
                    }
                    return ResponseEntity.ok(Result.success(application));
                } else {
                    // 直接批准普通申请
                    AssetApplication application = assetApplicationService.approveApplication(
                            id,
                            approvalRequest.getApproverId(),
                            approvalRequest.getApproverName(),
                            approvalRequest.getApprovalRemark());
                    if (application == null) {
                        return ResponseEntity.ok(Result.error(404, "申请不存在"));
                    }
                    return ResponseEntity.ok(Result.success(application));
                }
            } else {
                // 资产管理员批准
                // 只有状态为 leader_approved 的申请才能被资产管理员批准（忽略大小写）
                String currentStatus = existingApplication.getStatus();
                if (currentStatus != null && "leader_approved".equalsIgnoreCase(currentStatus)) {
                    // 资产管理员最终批准
                    AssetApplication application = assetApplicationService.approveApplication(
                            id,
                            approvalRequest.getApproverId(),
                            approvalRequest.getApproverName(),
                            approvalRequest.getApprovalRemark());
                    if (application == null) {
                        return ResponseEntity.ok(Result.error(404, "申请不存在"));
                    }
                    return ResponseEntity.ok(Result.success(application));
                } else {
                    // 状态不是 leader_approved，不能批准
                    return ResponseEntity.ok(Result.error(400, "资产报废申请需要领导先批准"));
                }
            }
        } else {
            // 其他类型申请，直接批准
            AssetApplication application = assetApplicationService.approveApplication(
                    id,
                    approvalRequest.getApproverId(),
                    approvalRequest.getApproverName(),
                    approvalRequest.getApprovalRemark());
            if (application == null) {
                return ResponseEntity.ok(Result.error(404, "申请不存在"));
            }
            return ResponseEntity.ok(Result.success(application));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Result<AssetApplication>> rejectApplication(
            @PathVariable Long id,
            @RequestBody ApprovalRequest approvalRequest) {
        AssetApplication application = assetApplicationService.rejectApplication(
                id,
                approvalRequest.getApproverId(),
                approvalRequest.getApproverName(),
                approvalRequest.getApprovalRemark());
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

    @PostMapping("/create-final-approval")
    public ResponseEntity<Result<AssetApplication>> createFinalApproval(
            @RequestBody AssetApplication finalApprovalRequest) {
        // 获取原申请ID
        Long originalApplicationId = finalApprovalRequest.getOriginalApplicationId();

        // 创建终审申请，状态设置为 pending_leader
        finalApprovalRequest.setStatus("pending_leader");
        finalApprovalRequest.setApplicationDate(LocalDateTime.now());

        // 保存终审申请
        AssetApplication finalApproval = assetApplicationService.createApplication(finalApprovalRequest);

        // 更新原申请状态为已提交终审，避免重复创建
        if (originalApplicationId != null) {
            assetApplicationService.updateStatus(originalApplicationId, "final_approval_created");
        }

        return ResponseEntity.ok(Result.success(finalApproval));
    }

    // 开始维修
    @PutMapping("/{id}/start-maintenance")
    public ResponseEntity<Result<AssetApplication>> startMaintenance(
            @PathVariable Long id,
            @RequestBody MaintenanceRequest request) {
        AssetApplication application = assetApplicationService.startMaintenance(id, request.getUserId(),
                request.getUserName());
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在或状态不允许"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

    // 结束维修
    @PostMapping("/{id}/complete-maintenance")
    public ResponseEntity<Result<AssetApplication>> completeMaintenance(
            @PathVariable Long id,
            @RequestBody MaintenanceRequest request) {
        AssetApplication application = assetApplicationService.completeMaintenance(id, request.getUserId(),
                request.getUserName());
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在或状态不允许"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

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