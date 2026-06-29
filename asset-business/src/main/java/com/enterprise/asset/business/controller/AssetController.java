package com.enterprise.asset.business.controller;

import com.enterprise.asset.common.util.Result;
import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.service.AssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 资产控制器 - 处理资产的CRUD操作及状态管理
 * 
 * TODO: 需要改造AssetService，因为它依赖auth模块的User实体和SecurityContext
 *       所有依赖User查询的方法需要改为通过Feign调用auth服务
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    /**
     * GET /api/assets - 获取所有资产列表
     * 
     * @return 资产列表
     */
    @GetMapping
    public ResponseEntity<Result<List<Asset>>> getAllAssets() {
        List<Asset> assets = assetService.getAllAssets();
        return ResponseEntity.ok(Result.success(assets));
    }

    /**
     * GET /api/assets/{id} - 根据ID获取资产详情
     * 
     * @param id 资产ID
     * @return 资产详情
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
     * POST /api/assets - 创建新资产
     * 
     * @param asset 资产实体（包含名称、编号、类别、购置价格等）
     * @return 创建后的资产
     */
    @PostMapping
    public ResponseEntity<Result<Asset>> createAsset(@RequestBody Asset asset) {
        Asset createdAsset = assetService.createAsset(asset);
        return ResponseEntity.ok(Result.success(createdAsset));
    }

    /**
     * PUT /api/assets/{id} - 更新资产信息
     * 
     * @param id    资产ID
     * @param asset 更新的资产数据
     * @return 更新后的资产
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
     * DELETE /api/assets/{id} - 删除资产
     * 
     * @param id 资产ID
     * @return 删除结果
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
     * PUT /api/assets/{id}/status - 更新资产状态
     * 
     * @param id           资产ID
     * @param statusUpdate 状态更新请求体 {"status": "..."}
     * @return 更新后的资产
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

    /** 状态更新请求体 */
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
     * PUT /api/assets/{id}/use-status - 更新资产使用状态
     * 
     * @param id              资产ID
     * @param useStatusUpdate 使用状态更新请求体 {"useStatus": "..."}
     * @return 更新后的资产
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

    /** 使用状态更新请求体 */
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
     * GET /api/assets/department/{departmentId} - 获取指定部门资产
     * 
     * @param departmentId 部门ID
     * @return 资产列表
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByDepartment(@PathVariable Long departmentId) {
        List<Asset> assets = assetService.getAllAssets();
        return ResponseEntity.ok(Result.success(assets));
    }

    /**
     * GET /api/assets/user/{userId} - 获取用户名下资产
     * 
     * @param userId 用户ID
     * @return 用户资产列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByUser(@PathVariable Long userId) {
        List<Asset> assets = assetService.getAssetsByUser(userId);
        return ResponseEntity.ok(Result.success(assets));
    }

    /**
     * GET /api/assets/status/{status} - 按状态筛选资产
     * 
     * @param status 资产状态 (using/maintenance/scrapped等)
     * @return 资产列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByStatus(@PathVariable String status) {
        List<Asset> assets = assetService.getAssetsByStatus(status);
        return ResponseEntity.ok(Result.success(assets));
    }

    /**
     * GET /api/assets/use-status/{useStatus} - 按使用状态筛选资产
     * 
     * @param useStatus 使用状态
     * @return 资产列表
     */
    @GetMapping("/use-status/{useStatus}")
    public ResponseEntity<Result<List<Asset>>> getAssetsByUseStatus(@PathVariable String useStatus) {
        List<Asset> assets = assetService.getAssetsByUseStatus(useStatus);
        return ResponseEntity.ok(Result.success(assets));
    }

    /**
     * GET /api/assets/count - 获取资产总数
     * 
     * @return 资产数量
     */
    @GetMapping("/count")
    public ResponseEntity<Result<Long>> getAssetCount() {
        long count = assetService.getAssetCount();
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * GET /api/assets/count/department/{departmentId} - 获取部门资产数量
     * 
     * @param departmentId 部门ID
     * @return 资产数量
     */
    @GetMapping("/count/department/{departmentId}")
    public ResponseEntity<Result<Long>> getAssetCountByDepartment(@PathVariable Long departmentId) {
        long count = assetService.getAssetCount();
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * GET /api/assets/count/user/{userId} - 获取用户资产数量
     * 
     * @param userId 用户ID
     * @return 资产数量
     */
    @GetMapping("/count/user/{userId}")
    public ResponseEntity<Result<Long>> getAssetCountByUser(@PathVariable Long userId) {
        long count = assetService.getAssetCountByUser(userId);
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * GET /api/assets/locations - 获取所有资产地点列表
     * 
     * @return 地点列表
     */
    @GetMapping("/locations")
    public ResponseEntity<Result<List<String>>> getAllLocations() {
        List<String> locations = assetService.getAllLocations();
        return ResponseEntity.ok(Result.success(locations));
    }

    /**
     * GET /api/assets/available - 获取可领用资产（未分配状态）
     * 
     * @return 可领用资产列表
     */
    @GetMapping("/available")
    public ResponseEntity<Result<List<Asset>>> getAvailableAssets() {
        List<Asset> assets = assetService.getAvailableAssets();
        return ResponseEntity.ok(Result.success(assets));
    }
}