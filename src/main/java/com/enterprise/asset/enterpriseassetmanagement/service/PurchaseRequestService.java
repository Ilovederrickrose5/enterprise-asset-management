package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.entity.PurchaseRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PurchaseRequestService {
    
    List<PurchaseRequest> getAllRequests();
    
    PurchaseRequest getRequestById(Long id);
    
    List<PurchaseRequest> getRequestsByApplicantId(Long applicantId);
    
    List<PurchaseRequest> getRequestsByDepartmentId(Long departmentId);
    
    List<PurchaseRequest> getRequestsByStatus(String status);
    
    List<PurchaseRequest> getRequestsByApplicantIdAndStatus(Long applicantId, String status);
    
    List<PurchaseRequest> getPendingRequestsForApprover(Long userId);
    
    PurchaseRequest createRequest(PurchaseRequest request);
    
    PurchaseRequest updateRequest(Long id, PurchaseRequest request);
    
    boolean deleteRequest(Long id);
    
    PurchaseRequest approveRequest(Long id, Long approverId, String approverName, String approvalRemark);
    
    PurchaseRequest rejectRequest(Long id, Long approverId, String approverName, String approvalRemark);
    
    // 分页查询方法
    Page<PurchaseRequest> getAllRequests(int page, int size, String status);
    
    Page<PurchaseRequest> getRequestsByApplicantId(Long applicantId, int page, int size, String status);
    
    Page<PurchaseRequest> getRequestsByDepartmentId(Long departmentId, int page, int size, String status);
    
    // 获取当前用户审批过的历史记录
    List<PurchaseRequest> getApprovedRequestsByApproverId(Long approverId);
}