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

/**
 * 资产业务申请控制器
 * <p>
 * 负责处理资产领用、转移、维修、报废四类申请的 REST API 请求
 * <p>
 * 角色职责：
 * - 接收前端 HTTP 请求（像快递网点前台接收快递单）
 * - 参数校验和格式转换
 * - 将请求转发给 Service 层处理业务逻辑
 * - 包装响应并返回给前端
 */
@RestController
@RequestMapping("/api/asset-applications")
public class AssetApplicationController {

    /**
     * 资产申请服务层（依赖注入）
     * 负责实际业务逻辑处理
     */
    @Autowired
    private AssetApplicationService assetApplicationService;

    /**
     * 获取资产申请列表（分页）
     * 
     * @param page   页码（默认0）
     * @param size   每页数量（默认10）
     * @param type   申请类型筛选（可选：RECEIVE/TRANSFER/MAINTENANCE/DISPOSAL）
     * @param status 状态筛选（可选：pending/approved/rejected等）
     * @return 分页的申请列表
     */
    @GetMapping
    public ResponseEntity<Result<Page<AssetApplication>>> getAllApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        Page<AssetApplication> applications;
        // 根据条件筛选查询
        if ((type != null && !type.isEmpty()) || (status != null && !status.isEmpty())) {
            applications = assetApplicationService.getApplicationsByTypeAndStatus(type, status, page, size);
        } else {
            applications = assetApplicationService.getAllApplications(page, size);
        }
        return ResponseEntity.ok(Result.success(applications));
    }

    /**
     * 根据ID获取单个申请详情
     * 
     * @param id 申请ID
     * @return 申请详情，如果不存在返回404错误
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<AssetApplication>> getApplicationById(@PathVariable Long id) {
        AssetApplication application = assetApplicationService.getApplicationById(id);
        if (application == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        return ResponseEntity.ok(Result.success(application));
    }

    /**
     * 根据申请人ID获取申请列表（分页）
     * 
     * @param applicantId 申请人ID
     * @param page        页码
     * @param size        每页数量
     * @param type        申请类型筛选（可选）
     * @param status      状态筛选（可选）
     * @return 分页的申请列表
     */
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

    /**
     * 根据状态获取申请列表
     * 
     * @param status 申请状态（pending/approved/rejected/pending_leader等）
     * @return 申请列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsByStatus(@PathVariable String status) {
        List<AssetApplication> applications = assetApplicationService.getApplicationsByStatus(status);
        return ResponseEntity.ok(Result.success(applications));
    }

    /**
     * 根据状态和部门获取申请列表
     * 
     * @param status       申请状态
     * @param departmentId 部门ID
     * @return 申请列表
     */
    @GetMapping("/status/{status}/department/{departmentId}")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsByStatusAndDepartment(
            @PathVariable String status, @PathVariable Long departmentId) {
        List<AssetApplication> applications = assetApplicationService.getApplicationsByStatusAndDepartment(status,
                departmentId);
        return ResponseEntity.ok(Result.success(applications));
    }

    /**
     * 获取等待领导审批的申请列表
     * 
     * @return 等待领导审批的申请列表
     */
    @GetMapping("/status/pending_leader")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsPendingLeader() {
        List<AssetApplication> applications = assetApplicationService.getApplicationsByStatus("pending_leader");
        return ResponseEntity.ok(Result.success(applications));
    }

    /**
     * 获取指定部门等待领导审批的申请列表
     * 
     * @param departmentId 部门ID
     * @return 等待领导审批的申请列表
     */
    @GetMapping("/status/pending_leader/department/{departmentId}")
    public ResponseEntity<Result<List<AssetApplication>>> getApplicationsPendingLeaderByDepartment(
            @PathVariable Long departmentId) {
        List<AssetApplication> applications = assetApplicationService
                .getApplicationsByStatusAndDepartment("pending_leader", departmentId);
        return ResponseEntity.ok(Result.success(applications));
    }

    /**
     * 根据部门获取申请列表（分页）
     * 
     * @param departmentId 部门ID
     * @param page         页码
     * @param size         每页数量
     * @param type         申请类型筛选（可选）
     * @param status       状态筛选（可选）
     * @return 分页的申请列表
     */
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

    /**
     * 创建资产申请（支持四类申请）
     * <p>
     * 通过 applicationType 字段区分申请类型：
     * - RECEIVE: 资产领用
     * - TRANSFER: 资产转移
     * - MAINTENANCE: 资产维修
     * - DISPOSAL: 资产报废
     * 
     * @param application 申请对象（包含申请类型和相关信息）
     * @return 创建成功的申请记录
     */
    @PostMapping
    public ResponseEntity<Result<AssetApplication>> createApplication(@RequestBody AssetApplication application) {
        AssetApplication createdApplication = assetApplicationService.createApplication(application);
        return ResponseEntity.ok(Result.success(createdApplication));
    }

    /**
     * 更新申请信息
     * 
     * @param id          申请ID
     * @param application 更新后的申请信息
     * @return 更新后的申请记录，如果不存在返回404错误
     */
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

    /**
     * 删除申请
     * 
     * @param id 申请ID
     * @return 删除结果，如果不存在返回404错误
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteApplication(@PathVariable Long id) {
        boolean deleted = assetApplicationService.deleteApplication(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    /**
     * 批准申请
     * <p>
     * 审批流程：
     * - 普通申请（领用/转移/维修）：资产管理员直接批准
     * - 报废申请：需要二级审批
     * 1. 领导（或系统管理员）先批准
     * 2. 资产管理员最终批准
     * 
     * @param id              申请ID
     * @param approvalRequest 审批信息（审批人ID、审批人姓名、审批备注）
     * @return 审批后的申请记录
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Result<AssetApplication>> approveApplication(
            @PathVariable Long id,
            @RequestBody ApprovalRequest approvalRequest) {
        // 获取当前用户信息（用于判断角色权限）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String userRole = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .findFirst()
                .orElse("");

        // 检查申请是否存在
        AssetApplication existingApplication = assetApplicationService.getApplicationById(id);
        if (existingApplication == null) {
            return ResponseEntity.ok(Result.error(404, "申请不存在"));
        }

        // 报废申请特殊处理（二级审批）
        if ("DISPOSAL".equals(existingApplication.getApplicationType())) {
            // 领导或系统管理员批准
            if ("ROLE_admin".equals(userRole) || "ROLE_leader".equals(userRole)) {
                // 如果是等待领导审批的终审申请
                if ("pending_leader".equals(existingApplication.getStatus())) {
                    // 批准终审申请
                    AssetApplication application = assetApplicationService.approveApplication(
                            id,
                            approvalRequest.getApproverId(),
                            approvalRequest.getApproverName(),
                            approvalRequest.getApprovalRemark());

                    // 更新原申请状态为领导已批准
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
                // 资产管理员批准（必须先经领导批准）
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
                    // 状态不是领导已批准，不能批准
                    return ResponseEntity.ok(Result.error(400, "资产报废申请需要领导先批准"));
                }
            }
        } else {
            // 其他类型申请（领用/转移/维修），资产管理员直接批准
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

    /**
     * 拒绝申请
     * 
     * @param id              申请ID
     * @param approvalRequest 审批信息（审批人ID、审批人姓名、拒绝原因）
     * @return 拒绝后的申请记录
     */
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

    /**
     * 创建报废终审申请
     * <p>
     * 报废申请需要二级审批，此方法用于创建需要领导审批的终审申请
     * 
     * @param finalApprovalRequest 终审申请信息（需包含原申请ID）
     * @return 创建成功的终审申请
     */
    @PostMapping("/create-final-approval")
    public ResponseEntity<Result<AssetApplication>> createFinalApproval(
            @RequestBody AssetApplication finalApprovalRequest) {
        // 获取原申请ID
        Long originalApplicationId = finalApprovalRequest.getOriginalApplicationId();

        // 创建终审申请，状态设置为等待领导审批
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

    /**
     * 开始维修
     * <p>
     * 维修申请批准后，可调用此接口开始维修，同步更新资产状态为维修中
     * 
     * @param id      申请ID
     * @param request 操作人信息
     * @return 更新后的申请记录
     */
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

    /**
     * 完成维修
     * <p>
     * 维修完成后，可调用此接口结束维修，同步更新资产状态为正常使用
     * 
     * @param id      申请ID
     * @param request 操作人信息
     * @return 更新后的申请记录
     */
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

    /**
     * 维修操作请求内部类
     * <p>
     * 用于开始维修和完成维修接口的请求参数
     */
    static class MaintenanceRequest {
        /** 操作人ID */
        private Long userId;
        /** 操作人姓名 */
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

    /**
     * 审批请求内部类
     * <p>
     * 用于批准和拒绝申请接口的请求参数
     */
    static class ApprovalRequest {
        /** 审批人ID */
        private Long approverId;
        /** 审批人姓名 */
        private String approverName;
        /** 审批备注/拒绝原因 */
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