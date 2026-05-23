package com.enterprise.asset.enterpriseassetmanagement.service;

import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.DepreciationRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 折旧计算器接口
 * 定义了不同折旧方法的计算逻辑
 */
public interface DepreciationCalculator {

    DepreciationRecord calculateDepreciation(
            Asset asset,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            BigDecimal accumulatedDepreciation,
            Integer usedMonths);

    default DepreciationRecord calculateDepreciation(
            Asset asset,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            BigDecimal accumulatedDepreciation,
            Integer usedMonths,
            BigDecimal actualWorkUnits) {
        return calculateDepreciation(asset, periodStartDate, periodEndDate, accumulatedDepreciation, usedMonths);
    }

    String getDepreciationMethod();

    BigDecimal calculateNetValue(BigDecimal originalValue, BigDecimal accumulatedDepreciation);
}