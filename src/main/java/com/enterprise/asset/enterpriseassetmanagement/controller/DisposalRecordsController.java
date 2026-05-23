package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.entity.AssetApplication;
import com.enterprise.asset.enterpriseassetmanagement.entity.User;
import com.enterprise.asset.enterpriseassetmanagement.repository.UserRepository;
import com.enterprise.asset.enterpriseassetmanagement.service.AssetApplicationService;
import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/disposal-records")
public class DisposalRecordsController {

    @Autowired
    private AssetApplicationService applicationService;

    @Autowired
    private UserRepository userRepository;

    // 获取报废记录（支持分页和部门筛选）
    @GetMapping
    public Result<PageResult<AssetApplication>> getDisposalRecords(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.error(401, "用户未认证");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 获取所有报废申请
        List<AssetApplication> allApplications = applicationService.getAllApplications();
        List<AssetApplication> disposalApplications = allApplications.stream()
                .filter(app -> "DISPOSAL".equals(app.getApplicationType()))
                .collect(Collectors.toList());

        // 根据用户角色和部门进行过滤
        List<AssetApplication> filteredApplications = disposalApplications.stream()
                .filter(application -> {
                    // 系统管理员可以查看所有记录
                    if ("admin".equals(user.getRole())) {
                        return true;
                    }

                    // 领导和部门资产管理员只能查看本部门记录
                    if (("leader".equals(user.getRole()) || "manager".equals(user.getRole()))
                            && user.getDeptId() != null) {
                        return application.getDepartmentId() != null
                                && application.getDepartmentId().equals(user.getDeptId());
                    }

                    // 普通员工只能查看自己的记录
                    return application.getApplicantId() != null && application.getApplicantId().equals(user.getId());
                })
                .collect(Collectors.toList());

        // 部门筛选
        if (departmentId != null) {
            filteredApplications = filteredApplications.stream()
                    .filter(application -> application.getDepartmentId() != null
                            && application.getDepartmentId().equals(departmentId))
                    .collect(Collectors.toList());
        }

        // 分页处理
        int total = filteredApplications.size();

        // 边界检查：确保page和pageSize为正数
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }

        int start = (page - 1) * pageSize;
        // 边界检查：确保start不超过total
        if (start >= total) {
            start = total;
        }

        List<AssetApplication> paginatedApplications = filteredApplications.stream()
                .skip(start)
                .limit(pageSize)
                .collect(Collectors.toList());

        PageResult<AssetApplication> result = new PageResult<>();
        result.setRecords(paginatedApplications);
        result.setTotal(total);
        result.setSize(pageSize);
        result.setCurrent(page);

        return Result.success(result);
    }

    // 分页结果类
    public static class PageResult<T> {
        private List<T> records;
        private long total;
        private int size;
        private int current;

        // getters and setters
        public List<T> getRecords() {
            return records;
        }

        public void setRecords(List<T> records) {
            this.records = records;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getCurrent() {
            return current;
        }

        public void setCurrent(int current) {
            this.current = current;
        }
    }
}