package com.enterprise.asset.enterpriseassetmanagement.controller;

import com.enterprise.asset.enterpriseassetmanagement.common.Result;
import com.enterprise.asset.enterpriseassetmanagement.entity.DepreciationRecord;
import com.enterprise.asset.enterpriseassetmanagement.service.DepreciationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/depreciation")
public class DepreciationController {

    @Autowired
    private DepreciationService depreciationService;

    @PostMapping("/calculate/{assetId}")
    public ResponseEntity<Result<DepreciationRecord>> calculateAssetDepreciation(
            @PathVariable Long assetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String depreciationMethod,
            @RequestParam(required = false) Integer actualWorkUnits) {
        try {
            DepreciationRecord record = depreciationService.calculateAssetDepreciation(assetId, startDate, endDate,
                    depreciationMethod, actualWorkUnits);
            return ResponseEntity.ok(Result.success(record));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    @PostMapping("/calculate/batch")
    public ResponseEntity<Result<List<DepreciationRecord>>> calculateBatchDepreciation(
            @RequestBody List<Long> assetIds,
            @RequestParam String depreciationMonth,
            @RequestParam(required = false) String depreciationMethod) {
        try {
            List<DepreciationRecord> records = depreciationService.calculateBatchDepreciation(assetIds,
                    depreciationMonth, depreciationMethod);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    @PostMapping("/calculate/all")
    public ResponseEntity<Result<List<DepreciationRecord>>> calculateAllAssetsDepreciation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationRecord> records = depreciationService.calculateAllAssetsDepreciation(startDate, endDate);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    @GetMapping("/records/{recordId}")
    public ResponseEntity<Result<DepreciationRecord>> getDepreciationRecordById(@PathVariable Long recordId) {
        try {
            DepreciationRecord record = depreciationService.getDepreciationRecordById(recordId);
            return ResponseEntity.ok(Result.success(record));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    @GetMapping("/records/asset/{assetId}")
    public ResponseEntity<Result<List<DepreciationRecord>>> getDepreciationRecordsByAssetId(
            @PathVariable Long assetId) {
        try {
            List<DepreciationRecord> records = depreciationService.getDepreciationRecordsByAssetId(assetId);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    @GetMapping("/records/asset/{assetId}/latest")
    public ResponseEntity<Result<DepreciationRecord>> getLatestDepreciationRecord(@PathVariable Long assetId) {
        try {
            DepreciationRecord record = depreciationService.getLatestDepreciationRecord(assetId);
            return ResponseEntity.ok(Result.success(record));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    @GetMapping("/records/date-range")
    public ResponseEntity<Result<List<DepreciationRecord>>> getDepreciationRecordsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DepreciationRecord> records = depreciationService.getDepreciationRecordsByDateRange(startDate,
                    endDate);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    @GetMapping("/records/category/{categoryId}")
    public ResponseEntity<Result<List<DepreciationRecord>>> getDepreciationRecordsByCategory(
            @PathVariable Long categoryId) {
        try {
            List<DepreciationRecord> records = depreciationService.getDepreciationRecordsByCategory(categoryId);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    @GetMapping("/records/department/{departmentId}")
    public ResponseEntity<Result<List<DepreciationRecord>>> getDepreciationRecordsByDepartment(
            @PathVariable Long departmentId) {
        try {
            List<DepreciationRecord> records = depreciationService.getDepreciationRecordsByDepartment(departmentId);
            return ResponseEntity.ok(Result.success(records));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }

    @DeleteMapping("/records/{recordId}/rollback")
    public ResponseEntity<Result<String>> rollbackDepreciationRecord(@PathVariable Long recordId) {
        try {
            depreciationService.rollbackDepreciationRecord(recordId);
            return ResponseEntity.ok(Result.success("回滚成功"));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, e.getMessage()));
        }
    }
}