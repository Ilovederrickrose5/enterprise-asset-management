package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.entity.AssetCategory;
import com.enterprise.asset.enterpriseassetmanagement.service.AssetCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset-categories")
public class AssetCategoryController {
    
    @Autowired
    private AssetCategoryService assetCategoryService;
    
    @GetMapping
    public ResponseEntity<Result<List<AssetCategory>>> getAllCategories() {
        List<AssetCategory> categories = assetCategoryService.getAllCategories();
        return ResponseEntity.ok(Result.success(categories));
    }
    
    @GetMapping("/active")
    public ResponseEntity<Result<List<AssetCategory>>> getActiveCategories() {
        List<AssetCategory> categories = assetCategoryService.getActiveCategories();
        return ResponseEntity.ok(Result.success(categories));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Result<AssetCategory>> getCategoryById(@PathVariable Long id) {
        AssetCategory category = assetCategoryService.getCategoryById(id);
        if (category == null) {
            return ResponseEntity.ok(Result.error(404, "资产分类不存在"));
        }
        return ResponseEntity.ok(Result.success(category));
    }
    
    @GetMapping("/code/{categoryCode}")
    public ResponseEntity<Result<AssetCategory>> getCategoryByCode(@PathVariable String categoryCode) {
        AssetCategory category = assetCategoryService.getCategoryByCode(categoryCode);
        if (category == null) {
            return ResponseEntity.ok(Result.error(404, "资产分类不存在"));
        }
        return ResponseEntity.ok(Result.success(category));
    }
    
    @PostMapping
    public ResponseEntity<Result<AssetCategory>> createCategory(@RequestBody AssetCategory category) {
        AssetCategory createdCategory = assetCategoryService.createCategory(category);
        return ResponseEntity.ok(Result.success(createdCategory));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Result<AssetCategory>> updateCategory(
            @PathVariable Long id,
            @RequestBody AssetCategory category) {
        AssetCategory updatedCategory = assetCategoryService.updateCategory(id, category);
        if (updatedCategory == null) {
            return ResponseEntity.ok(Result.error(404, "资产分类不存在"));
        }
        return ResponseEntity.ok(Result.success(updatedCategory));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteCategory(@PathVariable Long id) {
        boolean deleted = assetCategoryService.deleteCategory(id);
        if (!deleted) {
            return ResponseEntity.ok(Result.error(404, "资产分类不存在"));
        }
        return ResponseEntity.ok(Result.success("删除成功"));
    }
    
    @GetMapping("/count")
    public ResponseEntity<Result<Long>> getActiveCategoryCount() {
        long count = assetCategoryService.getActiveCategoryCount();
        return ResponseEntity.ok(Result.success(count));
    }
    
    @GetMapping("/generate-code")
    public ResponseEntity<Result<String>> generateCategoryCode(@RequestParam("parentId") Long parentId) {
        String categoryCode = assetCategoryService.generateCategoryCode(parentId);
        return ResponseEntity.ok(Result.success(categoryCode));
    }
}
