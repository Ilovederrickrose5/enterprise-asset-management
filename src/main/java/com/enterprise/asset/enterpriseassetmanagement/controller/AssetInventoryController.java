package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.entity.AssetInventory;
import com.enterprise.asset.enterpriseassetmanagement.entity.AssetInventoryDetail;
import com.enterprise.asset.enterpriseassetmanagement.service.AssetInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset-inventory")
public class AssetInventoryController {

    @Autowired
    private AssetInventoryService assetInventoryService;

    // 获取所有盘点计划
    @GetMapping
    public ResponseEntity<Result<List<AssetInventory>>> getAllPlans() {
        List<AssetInventory> plans = assetInventoryService.getAllPlans();
        return ResponseEntity.ok(Result.success(plans));
    }

    // 根据ID获取盘点计划
    @GetMapping("/{id}")
    public ResponseEntity<Result<AssetInventory>> getPlanById(@PathVariable Long id) {
        AssetInventory plan = assetInventoryService.getPlanById(id);
        if (plan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(plan));
    }

    // 获取我创建的或分配给我的任务
    @GetMapping("/user/{userId}")
    public ResponseEntity<Result<List<AssetInventory>>> getPlansByCreatorOrAssignee(@PathVariable Long userId) {
        List<AssetInventory> plans = assetInventoryService.getPlansByCreatorOrAssignee(userId);
        return ResponseEntity.ok(Result.success(plans));
    }

    // 根据状态获取盘点计划
    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<AssetInventory>>> getPlansByStatus(@PathVariable String status) {
        List<AssetInventory> plans = assetInventoryService.getPlansByStatus(status);
        return ResponseEntity.ok(Result.success(plans));
    }

    // 根据创建人ID获取盘点计划
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<Result<List<AssetInventory>>> getPlansByCreatorId(@PathVariable Long creatorId) {
        List<AssetInventory> plans = assetInventoryService.getPlansByCreatorId(creatorId);
        return ResponseEntity.ok(Result.success(plans));
    }

    // 创建盘点计划
    @PostMapping
    public ResponseEntity<Result<AssetInventory>> createPlan(@RequestBody AssetInventory plan) {
        AssetInventory createdPlan = assetInventoryService.createPlan(plan);
        return ResponseEntity.ok(Result.success(createdPlan));
    }

    // 更新盘点计划
    @PutMapping("/{id}")
    public ResponseEntity<Result<AssetInventory>> updatePlan(@PathVariable Long id, @RequestBody AssetInventory plan) {
        AssetInventory updatedPlan = assetInventoryService.updatePlan(id, plan);
        if (updatedPlan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedPlan));
    }

    // 更新盘点计划状态
    @PutMapping("/{id}/status")
    public ResponseEntity<Result<AssetInventory>> updatePlanStatus(@PathVariable Long id, @RequestParam String status) {
        AssetInventory updatedPlan = assetInventoryService.updatePlanStatus(id, status);
        if (updatedPlan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedPlan));
    }

    // 删除盘点计划
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deletePlan(@PathVariable Long id) {
        boolean deleted = assetInventoryService.deletePlan(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    // 分配任务
    @PostMapping("/{id}/assign")
    public ResponseEntity<Result<AssetInventory>> assignTask(@PathVariable Long id,
            @RequestBody AssignTaskRequest request) {
        AssetInventory updatedPlan = assetInventoryService.assignTask(id, request.getAssigneeId(),
                request.getAssigneeName(), request.getInventoryArea());
        if (updatedPlan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedPlan));
    }

    // 开始任务
    @PostMapping("/{id}/start")
    public ResponseEntity<Result<AssetInventory>> startTask(@PathVariable Long id) {
        AssetInventory updatedPlan = assetInventoryService.startTask(id);
        if (updatedPlan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedPlan));
    }

    // 完成任务
    @PostMapping("/{id}/complete")
    public ResponseEntity<Result<AssetInventory>> completeTask(@PathVariable Long id) {
        AssetInventory updatedPlan = assetInventoryService.completeTask(id);
        if (updatedPlan == null) {
            return ResponseEntity.ok(Result.error(404, "盘点计划不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedPlan));
    }

    // 获取盘点明细
    @GetMapping("/{id}/details")
    public ResponseEntity<Result<List<AssetInventoryDetail>>> getInventoryDetails(@PathVariable Long id) {
        List<AssetInventoryDetail> details = assetInventoryService.getInventoryDetails(id);
        return ResponseEntity.ok(Result.success(details));
    }

    // 添加盘点明细
    @PostMapping("/{id}/details")
    public ResponseEntity<Result<AssetInventoryDetail>> addInventoryDetail(@PathVariable Long id,
            @RequestBody AssetInventoryDetail detail) {
        AssetInventoryDetail addedDetail = assetInventoryService.addInventoryDetail(id, detail);
        return ResponseEntity.ok(Result.success(addedDetail));
    }

    // 更新盘点明细
    @PutMapping("/details/{detailId}")
    public ResponseEntity<Result<AssetInventoryDetail>> updateInventoryDetail(@PathVariable Long detailId,
            @RequestBody AssetInventoryDetail detail) {
        AssetInventoryDetail updatedDetail = assetInventoryService.updateInventoryDetail(detailId, detail);
        if (updatedDetail == null) {
            return ResponseEntity.ok(Result.error(404, "盘点明细不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedDetail));
    }

    // 删除盘点明细
    @DeleteMapping("/details/{detailId}")
    public ResponseEntity<Result<String>> deleteInventoryDetail(@PathVariable Long detailId) {
        boolean deleted = assetInventoryService.deleteInventoryDetail(detailId);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "盘点明细不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    // 分配任务请求类
    static class AssignTaskRequest {
        private Long assigneeId;
        private String assigneeName;
        private String inventoryArea;

        public Long getAssigneeId() {
            return assigneeId;
        }

        public void setAssigneeId(Long assigneeId) {
            this.assigneeId = assigneeId;
        }

        public String getAssigneeName() {
            return assigneeName;
        }

        public void setAssigneeName(String assigneeName) {
            this.assigneeName = assigneeName;
        }

        public String getInventoryArea() {
            return inventoryArea;
        }

        public void setInventoryArea(String inventoryArea) {
            this.inventoryArea = inventoryArea;
        }
    }
}
