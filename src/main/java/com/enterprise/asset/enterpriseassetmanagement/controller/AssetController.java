package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    @Autowired
    private AssetService assetService;

    /**
     * 获取所有资产
     */
    @GetMapping
    public ResponseEntity<Result<List<Asset>>> getAllAssets() {
        List<Asset> assets = assetService.getAllAssets();
        return ResponseEntity.ok(Result.success(assets));
    }

    /**
     * 根据ID获取资产
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<Asset>> getAssetById(@PathVariable Long id) {
        Asset asset = assetService.getAssetById(id);
        if (asset == null) {
            return ResponseEntity.ok(Result.error(404, "资产不存在"));
        }
        return ResponseEntity.ok(Result.success(asset));
    }

    /**
     * 创建资产
     */
    @PostMapping
    public ResponseEntity<Result<Asset>> createAsset(@RequestBody Asset asset) {
        Asset createdAsset = assetService.createAsset(asset);
        return ResponseEntity.ok(Result.success(createdAsset));
    }

    /**
     * 更新资产
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<Asset>> updateAsset(
            @PathVariable Long id,
            @RequestBody Asset asset) {
        Asset updatedAsset = assetService.updateAsset(id, asset);
        if (updatedAsset == null) {
            return ResponseEntity.ok(Result.error(404, "资产不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedAsset));
    }

    /**
     * 删除资产
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteAsset(@PathVariable Long id) {
        boolean deleted = assetService.deleteAsset(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "资产不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }

    /**
     * 更新资产状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Result<Asset>> updateAssetStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest statusUpdate) {
        try {
            Asset asset = assetService.updateAssetStatus(id, statusUpdate.getStatus());
            if (asset == null) {
                return ResponseEntity.ok(Result.error(404, "资产不存在"));
            }
            return ResponseEntity.ok(Result.success(asset));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Result.error(403, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "操作失败，请重试"));
        }
    }

    /**
     * 状态更新请求体
     */
    static class StatusUpdateRequest {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * 更新资产使用状态
     */
    @PutMapping("/{id}/use-status")
    public ResponseEntity<Result<Asset>> updateAssetUseStatus(
            @PathVariable Long id,
            @RequestBody UseStatusUpdateRequest useStatusUpdate) {
        Asset asset = assetService.updateAssetUseStatus(id, useStatusUpdate.getUseStatus());
        if (asset == null) {
            return ResponseEntity.ok(Result.error(404, "资产不存在"));
        }
        return ResponseEntity.ok(Result.success(asset));
    }

    /**
     * 使用状态更新请求体
     */
    static class UseStatusUpdateRequest {
        private String useStatus;

        public String getUseStatus() {
            return useStatus;
        }

        public void setUseStatus(String useStatus) {
            this.useStatus = useStatus;
        }
    }

    /**
     * 获取所有资产（不再按部门过滤）
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByDepartment(@PathVariable Long departmentId) {
        List<Asset> assets = assetService.getAllAssets();
        return ResponseEntity.ok(Result.success(assets));
    }

    /**
     * 根据用户ID获取资产
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByUser(@PathVariable Long userId) {
        List<Asset> assets = assetService.getAssetsByUser(userId);
        return ResponseEntity.ok(Result.success(assets));
    }

    /**
     * 根据状态获取资产
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByStatus(@PathVariable String status) {
        List<Asset> assets = assetService.getAssetsByStatus(status);
        return ResponseEntity.ok(Result.success(assets));
    }

    /**
     * 根据使用状态获取资产
     */
    @GetMapping("/use-status/{useStatus}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByUseStatus(@PathVariable String useStatus) {
        List<Asset> assets = assetService.getAssetsByUseStatus(useStatus);
        return ResponseEntity.ok(Result.success(assets));
    }

    /**
     * 获取资产总数
     */
    @GetMapping("/count")
    public ResponseEntity<Result<Long>> getAssetCount() {
        long count = assetService.getAssetCount();
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * 获取资产总数（不再按部门过滤）
     */
    @GetMapping("/count/department/{departmentId}")
    public ResponseEntity<Result<Long>> getAssetCountByDepartment(@PathVariable Long departmentId) {
        long count = assetService.getAssetCount();
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * 获取用户资产数量
     */
    @GetMapping("/count/user/{userId}")
    public ResponseEntity<Result<Long>> getAssetCountByUser(@PathVariable Long userId) {
        long count = assetService.getAssetCountByUser(userId);
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * 获取所有地点列表
     */
    @GetMapping("/locations")
    public ResponseEntity<Result<List<String>>> getAllLocations() {
        List<String> locations = assetService.getAllLocations();
        return ResponseEntity.ok(Result.success(locations));
    }

    /**
     * 获取可领用的资产（未分配给用户的资产）
     * 用于资产领用申请
     */
    @GetMapping("/available")
    public ResponseEntity<Result<List<Asset>>> getAvailableAssets() {
        List<Asset> assets = assetService.getAvailableAssets();
        return ResponseEntity.ok(Result.success(assets));
    }
}
