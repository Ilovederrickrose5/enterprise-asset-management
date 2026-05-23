package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.entity.PurchaseRequest;
import com.enterprise.asset.enterpriseassetmanagement.service.PurchaseRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-requests")
public class PurchaseRequestController {

    @Autowired
    private PurchaseRequestService purchaseRequestService;

    @GetMapping
    public ResponseEntity<Result<Page<PurchaseRequest>>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<PurchaseRequest> requests = purchaseRequestService.getAllRequests(page, size, status);
        return ResponseEntity.ok(Result.success(requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<PurchaseRequest>> getRequestById(@PathVariable Long id) {
        PurchaseRequest request = purchaseRequestService.getRequestById(id);
        if (request == null) {
            return ResponseEntity.ok(Result.error(404, "采购需求申请不存在"));
        }
        return ResponseEntity.ok(Result.success(request));
    }

    @GetMapping("/applicant/{applicantId}")
    public ResponseEntity<Result<Page<PurchaseRequest>>> getRequestsByApplicantId(
            @PathVariable Long applicantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<PurchaseRequest> requests = purchaseRequestService.getRequestsByApplicantId(applicantId, page, size, status);
        return ResponseEntity.ok(Result.success(requests));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Result<Page<PurchaseRequest>>> getRequestsByDepartmentId(
            @PathVariable Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<PurchaseRequest> requests = purchaseRequestService.getRequestsByDepartmentId(departmentId, page, size, status);
        return ResponseEntity.ok(Result.success(requests));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<PurchaseRequest>>> getRequestsByStatus(@PathVariable String status) {
        List<PurchaseRequest> requests = purchaseRequestService.getRequestsByStatus(status);
        return ResponseEntity.ok(Result.success(requests));
    }

    @GetMapping("/applicant/{applicantId}/status/{status}")
    public ResponseEntity<Result<List<PurchaseRequest>>> getRequestsByApplicantIdAndStatus(
            @PathVariable Long applicantId, 
            @PathVariable String status) {
        List<PurchaseRequest> requests = purchaseRequestService.getRequestsByApplicantIdAndStatus(applicantId, status);
        return ResponseEntity.ok(Result.success(requests));
    }

    @GetMapping("/pending")
    public ResponseEntity<Result<List<PurchaseRequest>>> getPendingRequests() {
        List<PurchaseRequest> requests = purchaseRequestService.getRequestsByStatus("pending");
        return ResponseEntity.ok(Result.success(requests));
    }

    @GetMapping("/pending/approver/{approverId}")
    public ResponseEntity<Result<List<PurchaseRequest>>> getPendingRequestsForApprover(@PathVariable Long approverId) {
        List<PurchaseRequest> requests = purchaseRequestService.getPendingRequestsForApprover(approverId);
        return ResponseEntity.ok(Result.success(requests));
    }

    @GetMapping("/approved/approver/{approverId}")
    public ResponseEntity<Result<List<PurchaseRequest>>> getApprovedRequestsByApprover(@PathVariable Long approverId) {
        List<PurchaseRequest> requests = purchaseRequestService.getApprovedRequestsByApproverId(approverId);
        return ResponseEntity.ok(Result.success(requests));
    }

    @PostMapping
    public ResponseEntity<Result<PurchaseRequest>> createRequest(@RequestBody PurchaseRequest request) {
        try {
            PurchaseRequest createdRequest = purchaseRequestService.createRequest(request);
            return ResponseEntity.ok(Result.success(createdRequest));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "创建采购需求申请失败：" + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<PurchaseRequest>> updateRequest(
            @PathVariable Long id,
            @RequestBody PurchaseRequest request) {
        try {
            PurchaseRequest updatedRequest = purchaseRequestService.updateRequest(id, request);
            if (updatedRequest == null) {
                return ResponseEntity.ok(Result.error(404, "采购需求申请不存在"));
            }
            return ResponseEntity.ok(Result.success(updatedRequest));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "更新采购需求申请失败：" + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteRequest(@PathVariable Long id) {
        try {
            boolean deleted = purchaseRequestService.deleteRequest(id);
            if (!deleted) {
                return ResponseEntity.ok(Result.error(404, "采购需求申请不存在"));
            }
            return ResponseEntity.ok(Result.success("删除成功"));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "删除采购需求申请失败：" + e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Result<PurchaseRequest>> approveRequest(
            @PathVariable Long id,
            @RequestBody ApprovalRequest approvalRequest) {
        try {
            PurchaseRequest request = purchaseRequestService.approveRequest(
                    id,
                    approvalRequest.getApproverId(),
                    approvalRequest.getApproverName(),
                    approvalRequest.getApprovalRemark()
            );
            if (request == null) {
                return ResponseEntity.ok(Result.error(404, "采购需求申请不存在"));
            }
            return ResponseEntity.ok(Result.success(request));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "审批失败：" + e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Result<PurchaseRequest>> rejectRequest(
            @PathVariable Long id,
            @RequestBody ApprovalRequest approvalRequest) {
        try {
            PurchaseRequest request = purchaseRequestService.rejectRequest(
                    id,
                    approvalRequest.getApproverId(),
                    approvalRequest.getApproverName(),
                    approvalRequest.getApprovalRemark()
            );
            if (request == null) {
                return ResponseEntity.ok(Result.error(404, "采购需求申请不存在"));
            }
            return ResponseEntity.ok(Result.success(request));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "审批失败：" + e.getMessage()));
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