package com.enterprise.asset.enterpriseassetmanagement.service.impl;

import com.enterprise.asset.enterpriseassetmanagement.common.ApplicationStatus;
import com.enterprise.asset.enterpriseassetmanagement.common.ApplicationType;
import com.enterprise.asset.enterpriseassetmanagement.common.AssetStatus;
import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.AssetApplication;
import com.enterprise.asset.enterpriseassetmanagement.entity.SysLog;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetApplicationRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.AssetRepository;
import com.enterprise.asset.enterpriseassetmanagement.repository.SysLogRepository;
import com.enterprise.asset.enterpriseassetmanagement.service.AssetApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 资产业务申请服务实现 - 处理业务逻辑 */
@Service
public class AssetApplicationServiceImpl implements AssetApplicationService {

    @Autowired
    private AssetApplicationRepository assetApplicationRepository;
    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    private SysLogRepository sysLogRepository;

    // === 查询方法 ===

    @Override
    public List<AssetApplication> getAllApplications() {
        return assetApplicationRepository.findAll();
    }

    @Override
    public List<AssetApplication> getAllApplicationsByDepartment(Long departmentId) {
        return assetApplicationRepository.findByDepartmentId(departmentId);
    }

    @Override
    public AssetApplication getApplicationById(Long id) {
        return assetApplicationRepository.findById(id).orElse(null);
    }

    @Override
    public List<AssetApplication> getApplicationsByApplicantId(Long applicantId) {
        return assetApplicationRepository.findByApplicantId(applicantId);
    }

    @Override
    public List<AssetApplication> getApplicationsByStatus(String status) {
        return assetApplicationRepository.findByStatus(status);
    }

    @Override
    public List<AssetApplication> getApplicationsByStatusAndDepartment(String status, Long departmentId) {
        return assetApplicationRepository.findByStatusAndDepartmentId(status, departmentId);
    }

    // === 核心业务流程 ===

    /**
     * 创建申请
     * Controller传入: AssetApplication对象(包含assetId、applicantId、applicationType等)
     * 业务逻辑: 设置申请时间→查询资产信息填充→保存申请→添加操作日志
     * 返回: 保存后的申请记录
     */
    @Override
    @Transactional
    public AssetApplication createApplication(AssetApplication application) {
        // 接收Controller传来的申请对象
        application.setApplicationDate(LocalDateTime.now());
        if (application.getStatus() == null || application.getStatus().isEmpty()) {
            application.setStatus(ApplicationStatus.PENDING.getCode());
        }

        // 查询资产表，填充资产名称和编号
        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            if (application.getAssetName() == null || application.getAssetName().isEmpty()) {
                application.setAssetName(asset.getAssetName());
            }
            if (application.getAssetNo() == null || application.getAssetNo().isEmpty()) {
                application.setAssetNo(asset.getAssetNo());
            }
        }

        // 保存申请到数据库
        AssetApplication savedApplication = assetApplicationRepository.save(application);

        // 添加操作日志到SysLog表
        if (asset != null) {
            SysLog log = new SysLog();
            log.setUserId(application.getApplicantId());
            log.setUsername(application.getApplicantName());
            log.setOperation("申请" + getTypeNameCN(application.getApplicationType()) + "：" + asset.getAssetName());
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);
        }

        return savedApplication;
    }

    @Override
    @Transactional
    public AssetApplication updateApplication(Long id, AssetApplication application) {
        AssetApplication existingApplication = assetApplicationRepository.findById(id).orElse(null);
        if (existingApplication == null) {
            return null;
        }
        existingApplication.setApplicationReason(application.getApplicationReason());
        existingApplication.setDepartmentId(application.getDepartmentId());
        existingApplication.setDepartmentName(application.getDepartmentName());
        return assetApplicationRepository.save(existingApplication);
    }

    @Override
    @Transactional
    public boolean deleteApplication(Long id) {
        if (assetApplicationRepository.existsById(id)) {
            assetApplicationRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * 批准申请
     * Controller传入:
     * id(申请ID)、approverId(审批人ID)、approverName(审批人姓名)、approvalRemark(审批备注)
     * 业务逻辑: 更新申请状态→根据申请类型更新资产状态(领用/转移/报废)→添加审批日志
     * 返回: 批准后的申请记录
     */
    @Override
    @Transactional
    public AssetApplication approveApplication(Long id, Long approverId, String approverName, String approvalRemark) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null) {
            return null;
        }

        // 更新申请状态为已批准
        application.setStatus(ApplicationStatus.APPROVED.getCode());
        application.setApprovalDate(LocalDateTime.now());
        application.setApproverId(approverId);
        application.setApproverName(approverName);
        application.setApprovalRemark(approvalRemark);
        AssetApplication savedApplication = assetApplicationRepository.save(application);

        // 根据申请类型更新资产状态
        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            ApplicationType type = ApplicationType.fromCode(application.getApplicationType());
            // 根据applicationType 区分业务逻辑
            if (type == ApplicationType.RECEIVE) {
                // 领用: 资产分配给申请人
                asset.setStatus(AssetStatus.USING.getCode());
                asset.setUseStatus(AssetStatus.USING.getCode());
                asset.setUserId(application.getApplicantId());
                asset.setDeptId(application.getDepartmentId());
            } else if (type == ApplicationType.TRANSFER) {
                // 转移: 资产分配给接收方
                asset.setStatus(AssetStatus.USING.getCode());
                asset.setUseStatus(AssetStatus.USING.getCode());
                asset.setUserId(application.getTransfereeId());
                asset.setDeptId(application.getTransfereeDeptId());
            } else if (type == ApplicationType.DISPOSAL) {
                // 报废: 资产状态改为报废
                asset.setStatus(AssetStatus.SCRAPPED.getCode());
                asset.setUseStatus(AssetStatus.SCRAPPED.getCode());
                if (application.getEstimatedValue() != null) {
                    asset.setPurchasePrice(application.getEstimatedValue());
                }
            } else if (type == ApplicationType.MAINTENANCE) {
                // 维修: 批准后资产状态不变，等待点击开始维修
            }
            // 保存资产状态变更(维修除外)
            if (type != ApplicationType.MAINTENANCE) {
                assetRepository.save(asset);
            }

            // 添加审批日志
            SysLog log = new SysLog();
            log.setUserId(approverId);
            log.setUsername(approverName);
            log.setOperation("批准" + getTypeNameCN(application.getApplicationType()) + "：" + asset.getAssetName());
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);

            // 转移申请额外添加双方日志
            if (type == ApplicationType.TRANSFER) {
                addTransferLogs(application, asset);
            }
        }

        return savedApplication;
    }

    @Override
    @Transactional
    public AssetApplication approveApplicationForLeader(Long id, Long approverId, String approverName,
            String approvalRemark) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null) {
            return null;
        }
        // 状态改为等待领导审批(二级审批流程)
        application.setStatus(ApplicationStatus.PENDING_LEADER.getCode());
        application.setApprovalDate(LocalDateTime.now());
        application.setApproverId(approverId);
        application.setApproverName(approverName);
        application.setApprovalRemark(approvalRemark);
        return assetApplicationRepository.save(application);
    }

    /**
     * 拒绝申请
     * Controller传入: id、approverId、approverName、approvalRemark
     * 业务逻辑: 更新申请状态为rejected→添加拒绝日志
     */
    @Override
    @Transactional
    public AssetApplication rejectApplication(Long id, Long approverId, String approverName, String approvalRemark) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null) {
            return null;
        }

        application.setStatus(ApplicationStatus.REJECTED.getCode());
        application.setApprovalDate(LocalDateTime.now());
        application.setApproverId(approverId);
        application.setApproverName(approverName);
        application.setApprovalRemark(approvalRemark);
        AssetApplication savedApplication = assetApplicationRepository.save(application);

        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            SysLog log = new SysLog();
            log.setUserId(approverId);
            log.setUsername(approverName);
            log.setOperation("拒绝" + getTypeNameCN(application.getApplicationType()) + "：" + asset.getAssetName());
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);
        }

        return savedApplication;
    }

    @Override
    @Transactional
    public AssetApplication updateStatus(Long id, String status) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null) {
            return null;
        }
        application.setStatus(status);
        return assetApplicationRepository.save(application);
    }

    /**
     * 开始维修
     * Controller传入: id(申请ID)、userId(操作人ID)、userName(操作人姓名)
     * 业务逻辑: 验证状态→更新申请为维修中→更新资产状态为maintenance→添加日志
     */
    @Override
    @Transactional
    public AssetApplication startMaintenance(Long id, Long userId, String userName) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null || !ApplicationStatus.APPROVED.getCode().equals(application.getStatus())) {
            return null;
        }

        application.setStatus(ApplicationStatus.IN_PROGRESS.getCode());
        AssetApplication savedApplication = assetApplicationRepository.save(application);

        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            asset.setStatus(AssetStatus.MAINTENANCE.getCode());
            asset.setUseStatus(AssetStatus.MAINTENANCE.getCode());
            assetRepository.save(asset);

            SysLog log = new SysLog();
            log.setUserId(userId);
            log.setUsername(userName);
            log.setOperation("开始维修：" + asset.getAssetName());
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);
        }

        return savedApplication;
    }

    /**
     * 完成维修
     * Controller传入: id、userId、userName
     * 业务逻辑: 验证状态→更新申请为已完成→更新资产状态为using→添加日志
     */
    @Override
    @Transactional
    public AssetApplication completeMaintenance(Long id, Long userId, String userName) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null || !ApplicationStatus.IN_PROGRESS.getCode().equals(application.getStatus())) {
            return null;
        }

        application.setStatus(ApplicationStatus.COMPLETED.getCode());
        AssetApplication savedApplication = assetApplicationRepository.save(application);

        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            asset.setStatus(AssetStatus.USING.getCode());
            asset.setUseStatus(AssetStatus.USING.getCode());
            assetRepository.save(asset);

            SysLog log = new SysLog();
            log.setUserId(userId);
            log.setUsername(userName);
            log.setOperation("完成维修：" + asset.getAssetName());
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);
        }

        return savedApplication;
    }

    // === 分页查询 ===

    @Override
    public Page<AssetApplication> getAllApplications(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "applicationDate"));
        return assetApplicationRepository.findAll(pageable);
    }

    @Override
    public Page<AssetApplication> getApplicationsByApplicantId(Long applicantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "applicationDate"));
        return assetApplicationRepository.findByApplicantId(applicantId, pageable);
    }

    @Override
    public Page<AssetApplication> getApplicationsByDepartmentId(Long departmentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "applicationDate"));
        return assetApplicationRepository.findByDepartmentId(departmentId, pageable);
    }

    @Override
    public Page<AssetApplication> getApplicationsByTypeAndStatus(String type, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "applicationDate"));
        return assetApplicationRepository.findByTypeAndStatus(type, status, pageable);
    }

    @Override
    public Page<AssetApplication> getApplicationsByApplicantIdAndTypeAndStatus(Long applicantId, String type,
            String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "applicationDate"));
        return assetApplicationRepository.findByApplicantIdAndTypeAndStatus(applicantId, type, status, pageable);
    }

    @Override
    public Page<AssetApplication> getApplicationsByDepartmentIdAndTypeAndStatus(Long departmentId, String type,
            String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "applicationDate"));
        return assetApplicationRepository.findByDepartmentIdAndTypeAndStatus(departmentId, type, status, pageable);
    }

    // === 工具方法 ===

    /** 申请类型英文转中文 */
    private String getTypeNameCN(String applicationType) {
        ApplicationType type = ApplicationType.fromCode(applicationType);
        if (type != null) {
            return "资产" + type.getName();
        }
        return applicationType;
    }

    /** 资产转移添加双方日志 */
    private void addTransferLogs(AssetApplication application, Asset asset) {
        SysLog transferorLog = new SysLog();
        transferorLog.setUserId(application.getApplicantId());
        transferorLog.setUsername(application.getApplicantName());
        String transfereeName = application.getTransfereeName() != null ? application.getTransfereeName() : "未知用户";
        transferorLog.setOperation("资产转移成功：已将" + asset.getAssetName() + "转移给" + transfereeName);
        transferorLog.setLogType("ASSET");
        transferorLog.setStatus("success");
        sysLogRepository.save(transferorLog);

        if (application.getTransfereeId() != null && application.getTransfereeName() != null) {
            SysLog transfereeLog = new SysLog();
            transfereeLog.setUserId(application.getTransfereeId());
            transfereeLog.setUsername(application.getTransfereeName());
            transfereeLog.setOperation("收到资产：" + application.getApplicantName() + "转移给您的" + asset.getAssetName());
            transfereeLog.setLogType("ASSET");
            transfereeLog.setStatus("success");
            sysLogRepository.save(transfereeLog);
        }
    }
}
