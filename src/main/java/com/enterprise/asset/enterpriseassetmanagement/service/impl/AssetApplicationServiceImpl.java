package com.enterprise.asset.enterpriseassetmanagement.service.impl;

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

@Service
public class AssetApplicationServiceImpl implements AssetApplicationService {

    @Autowired
    private AssetApplicationRepository assetApplicationRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private SysLogRepository sysLogRepository;

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
    @Transactional
    public AssetApplication createApplication(AssetApplication application) {
        application.setApplicationDate(LocalDateTime.now());
        // 如果状态为空，设置默认值为 pending；否则保留传入的状态（如 pending_leader）
        if (application.getStatus() == null || application.getStatus().isEmpty()) {
            application.setStatus("pending");
        }

        // 如果资产名称为空，从资产表中查询并填充
        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            if (application.getAssetName() == null || application.getAssetName().isEmpty()) {
                application.setAssetName(asset.getAssetName());
            }
            if (application.getAssetNo() == null || application.getAssetNo().isEmpty()) {
                application.setAssetNo(asset.getAssetNo());
            }
        }

        AssetApplication savedApplication = assetApplicationRepository.save(application);

        // 添加操作记录
        if (asset != null) {
            SysLog log = new SysLog();
            log.setUserId(application.getApplicantId());
            log.setUsername(application.getApplicantName());

            // 将英文申请类型转换为中文
            String applicationType = application.getApplicationType();
            String typeName = applicationType;
            if ("RECEIVE".equals(applicationType)) {
                typeName = "资产领用";
            } else if ("TRANSFER".equals(applicationType)) {
                typeName = "资产转移";
            } else if ("MAINTENANCE".equals(applicationType) || "REPAIR".equals(applicationType)) {
                typeName = "资产维修";
            } else if ("DISPOSAL".equals(applicationType)) {
                typeName = "资产报废";
            }

            log.setOperation("申请" + typeName + "：" + asset.getAssetName() + " ("
                    + asset.getAssetNo() + ")");
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

    @Override
    @Transactional
    public AssetApplication approveApplication(Long id, Long approverId, String approverName, String approvalRemark) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null) {
            return null;
        }

        application.setStatus("approved");
        application.setApprovalDate(LocalDateTime.now());
        application.setApproverId(approverId);
        application.setApproverName(approverName);
        application.setApprovalRemark(approvalRemark);

        AssetApplication savedApplication = assetApplicationRepository.save(application);

        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            // 根据申请类型设置不同的资产状态
            if ("RECEIVE".equals(application.getApplicationType())) {
                // 资产领用申请
                asset.setStatus("using");
                asset.setUseStatus("using");
                asset.setUserId(application.getApplicantId());
                asset.setDeptId(application.getDepartmentId());
            } else if ("TRANSFER".equals(application.getApplicationType())) {
                // 资产转移申请
                asset.setStatus("using");
                asset.setUseStatus("using");
                asset.setUserId(application.getTransfereeId());
                asset.setDeptId(application.getTransfereeDeptId());
            } else if ("DISPOSAL".equals(application.getApplicationType())) {
                // 资产报废申请
                asset.setStatus("scrapped");
                asset.setUseStatus("scrapped");
                // 如果有估计价值，更新资产价值
                if (application.getEstimatedValue() != null) {
                    asset.setPurchasePrice(application.getEstimatedValue());
                }
            } else if ("MAINTENANCE".equals(application.getApplicationType())
                    || "REPAIR".equals(application.getApplicationType())) {
                // 资产维修申请 - 批准后资产状态不变，等待用户点击开始维修
                // 资产状态将在用户点击"开始维修"按钮时更新
            }
            // 只有状态发生变化时才保存资产
            if (!("MAINTENANCE".equals(application.getApplicationType()) 
                    || "REPAIR".equals(application.getApplicationType()))) {
                assetRepository.save(asset);
            }

            // 添加审批操作记录
            SysLog log = new SysLog();
            log.setUserId(approverId);
            log.setUsername(approverName);

            // 将英文申请类型转换为中文
            String applicationType = application.getApplicationType();
            String typeName = applicationType;
            if ("RECEIVE".equals(applicationType)) {
                typeName = "资产领用";
            } else if ("TRANSFER".equals(applicationType)) {
                typeName = "资产转移";
            } else if ("MAINTENANCE".equals(applicationType) || "REPAIR".equals(applicationType)) {
                typeName = "资产维修";
            } else if ("DISPOSAL".equals(applicationType)) {
                typeName = "资产报废";
            }

            log.setOperation("批准" + typeName + "申请：" + asset.getAssetName() + " (" + asset.getAssetNo() + ")");
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);

            // 资产转移申请批准后，为转移方和接收方添加操作记录
            if ("TRANSFER".equals(application.getApplicationType())) {
                // 为转移方（申请人）添加操作记录
                SysLog transferorLog = new SysLog();
                transferorLog.setUserId(application.getApplicantId());
                transferorLog.setUsername(application.getApplicantName());
                String transfereeName = application.getTransfereeName() != null ? application.getTransfereeName() : "未知用户";
                transferorLog.setOperation("资产转移成功：已将资产 \"" + asset.getAssetName() + "\" 转移给 " + transfereeName);
                transferorLog.setLogType("ASSET");
                transferorLog.setStatus("success");
                sysLogRepository.save(transferorLog);

                // 为接收方添加操作记录
                if (application.getTransfereeId() != null && application.getTransfereeName() != null) {
                    SysLog transfereeLog = new SysLog();
                    transfereeLog.setUserId(application.getTransfereeId());
                    transfereeLog.setUsername(application.getTransfereeName());
                    transfereeLog.setOperation("收到资产：" + application.getApplicantName() + " 转移给您的资产 \"" + asset.getAssetName() + "\"");
                    transfereeLog.setLogType("ASSET");
                    transfereeLog.setStatus("success");
                    sysLogRepository.save(transfereeLog);
                }
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

        // 资产管理员批准后，状态改为等待领导审批
        application.setStatus("pending_leader");
        application.setApprovalDate(LocalDateTime.now());
        application.setApproverId(approverId);
        application.setApproverName(approverName);
        application.setApprovalRemark(approvalRemark);

        // 保存申请，但不更新资产状态，因为还需要领导终审
        return assetApplicationRepository.save(application);
    }

    @Override
    @Transactional
    public AssetApplication rejectApplication(Long id, Long approverId, String approverName, String approvalRemark) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null) {
            return null;
        }

        application.setStatus("rejected");
        application.setApprovalDate(LocalDateTime.now());
        application.setApproverId(approverId);
        application.setApproverName(approverName);
        application.setApprovalRemark(approvalRemark);

        AssetApplication savedApplication = assetApplicationRepository.save(application);

        // 添加拒绝操作记录
        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            SysLog log = new SysLog();
            log.setUserId(approverId);
            log.setUsername(approverName);

            String applicationType = application.getApplicationType();
            String typeName = applicationType;
            if ("RECEIVE".equals(applicationType)) {
                typeName = "资产领用";
            } else if ("TRANSFER".equals(applicationType)) {
                typeName = "资产转移";
            } else if ("MAINTENANCE".equals(applicationType) || "REPAIR".equals(applicationType)) {
                typeName = "资产维修";
            } else if ("DISPOSAL".equals(applicationType)) {
                typeName = "资产报废";
            }

            log.setOperation("拒绝" + typeName + "申请：" + asset.getAssetName() + " (" + asset.getAssetNo() + ")");
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

    @Override
    @Transactional
    public AssetApplication startMaintenance(Long id, Long userId, String userName) {
        try {
            AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
            if (application == null) {
                return null;
            }

            // 只有已批准的维修申请才能开始维修
            if (!"approved".equals(application.getStatus())) {
                return null;
            }

            // 更新申请状态为维修中
            application.setStatus("in_progress");
            AssetApplication savedApplication = assetApplicationRepository.save(application);

            // 更新资产状态为维修中
            Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
            if (asset != null) {
                asset.setStatus("maintenance");
                asset.setUseStatus("maintenance");
                assetRepository.save(asset);

                // 添加操作日志
                SysLog log = new SysLog();
                log.setUserId(userId);
                log.setUsername(userName != null ? userName : "");
                String assetName = asset.getAssetName() != null ? asset.getAssetName() : "";
                String assetNo = asset.getAssetNo() != null ? asset.getAssetNo() : "";
                log.setOperation("开始维修：" + assetName + " (" + assetNo + ")");
                log.setLogType("ASSET");
                log.setStatus("success");
                sysLogRepository.save(log);
            }

            return savedApplication;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional
    public AssetApplication completeMaintenance(Long id, Long userId, String userName) {
        AssetApplication application = assetApplicationRepository.findById(id).orElse(null);
        if (application == null) {
            return null;
        }

        // 只有维修中的申请才能结束维修
        if (!"in_progress".equals(application.getStatus())) {
            return null;
        }

        // 更新申请状态为已完成
        application.setStatus("completed");
        AssetApplication savedApplication = assetApplicationRepository.save(application);

        // 更新资产状态为使用中
        Asset asset = assetRepository.findById(application.getAssetId()).orElse(null);
        if (asset != null) {
            asset.setStatus("using");
            asset.setUseStatus("using");
            assetRepository.save(asset);

            // 添加操作日志
            SysLog log = new SysLog();
            log.setUserId(userId);
            log.setUsername(userName);
            log.setOperation("完成维修：" + asset.getAssetName() + " (" + asset.getAssetNo() + ")");
            log.setLogType("ASSET");
            log.setStatus("success");
            sysLogRepository.save(log);
        }

        return savedApplication;
    }

    // 分页查询实现
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
}