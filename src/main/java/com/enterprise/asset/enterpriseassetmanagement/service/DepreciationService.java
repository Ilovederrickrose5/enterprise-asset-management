package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.DepreciationRecord;

import java.time.LocalDate;
import java.util.List;

public interface DepreciationService {

        DepreciationRecord calculateAssetDepreciation(Long assetId, LocalDate startDate, LocalDate endDate);

        DepreciationRecord calculateAssetDepreciation(Long assetId, LocalDate startDate, LocalDate endDate,
                        java.math.BigDecimal actualWorkUnits);

        DepreciationRecord calculateAssetDepreciation(Long assetId, LocalDate startDate, LocalDate endDate,
                        String depreciationMethod, Integer actualWorkUnits);

        List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, LocalDate startDate,
                        LocalDate endDate);

        List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, LocalDate startDate, LocalDate endDate,
                        java.math.BigDecimal actualWorkUnits);

        List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, String depreciationMonth);

        List<DepreciationRecord> calculateBatchDepreciation(List<Long> assetIds, String depreciationMonth,
                        String depreciationMethod);

        List<DepreciationRecord> calculateAllAssetsDepreciation(LocalDate startDate, LocalDate endDate);

        DepreciationRecord getLatestDepreciationRecord(Long assetId);

        List<DepreciationRecord> getDepreciationRecordsByAssetId(Long assetId);

        List<DepreciationRecord> getDepreciationRecordsByDateRange(LocalDate startDate, LocalDate endDate);

        List<DepreciationRecord> getDepreciationRecordsByCategory(Long categoryId);

        List<DepreciationRecord> getDepreciationRecordsByDepartment(Long departmentId);

        boolean validateDepreciationCalculation(Asset asset, DepreciationRecord record);

        void rollbackDepreciationRecord(Long recordId);

        DepreciationRecord getDepreciationRecordById(Long recordId);
}