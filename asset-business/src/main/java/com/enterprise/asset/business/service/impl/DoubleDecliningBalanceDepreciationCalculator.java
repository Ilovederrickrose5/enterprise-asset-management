package com.enterprise.asset.business.service.impl;

import com.enterprise.asset.business.entity.Asset;
import com.enterprise.asset.business.entity.DepreciationRecord;
import com.enterprise.asset.business.service.DepreciationCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** 双倍余额递减法折旧计算器 - 前期折旧快后期慢的加速折旧方法 */
public class DoubleDecliningBalanceDepreciationCalculator implements DepreciationCalculator {
    
    private static final String DEPRECIATION_METHOD = "DOUBLE_DECLINING_BALANCE";
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final int DECIMAL_PLACES = 2;
    
    @Override
    public DepreciationRecord calculateDepreciation(
            Asset asset,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            BigDecimal accumulatedDepreciation,
            Integer usedMonths) {
        
        DepreciationRecord record = new DepreciationRecord();
        
        BigDecimal originalValue = asset.getOriginalValue() != null ? asset.getOriginalValue() : BigDecimal.ZERO;
        Integer usefulLife = asset.getUsefulLife() != null ? asset.getUsefulLife() : 0;
        
        if (usefulLife <= 0 || originalValue.compareTo(BigDecimal.ZERO) <= 0) {
            record.setDepreciationAmount(BigDecimal.ZERO);
            record.setCurrentNetValue(originalValue);
            record.setAccumulatedDepreciation(accumulatedDepreciation);
            return record;
        }
        
        BigDecimal currentNetValue = calculateNetValue(originalValue, accumulatedDepreciation);
        
        if (currentNetValue.compareTo(BigDecimal.ZERO) <= 0) {
            record.setDepreciationAmount(BigDecimal.ZERO);
            record.setCurrentNetValue(BigDecimal.ZERO);
            record.setAccumulatedDepreciation(originalValue);
            return record;
        }
        
        BigDecimal doubleRate = new BigDecimal(2).divide(
            new BigDecimal(usefulLife),
            DECIMAL_PLACES,
            ROUNDING_MODE
        );
        
        BigDecimal annualDepreciation = currentNetValue.multiply(doubleRate)
            .setScale(DECIMAL_PLACES, ROUNDING_MODE);
        
        long monthsInPeriod = ChronoUnit.MONTHS.between(periodStartDate, periodEndDate) + 1;
        BigDecimal periodDepreciation = annualDepreciation.divide(
            new BigDecimal(12),
            DECIMAL_PLACES,
            ROUNDING_MODE
        ).multiply(new BigDecimal(monthsInPeriod)).setScale(DECIMAL_PLACES, ROUNDING_MODE);
        
        BigDecimal newAccumulatedDepreciation = accumulatedDepreciation.add(periodDepreciation);
        BigDecimal newNetValue = originalValue.subtract(newAccumulatedDepreciation);
        
        if (newNetValue.compareTo(BigDecimal.ZERO) < 0) {
            newNetValue = BigDecimal.ZERO;
            periodDepreciation = originalValue.subtract(accumulatedDepreciation);
            newAccumulatedDepreciation = originalValue;
        }
        
        record.setAssetId(asset.getId());
        record.setAssetNo(asset.getAssetNo());
        record.setAssetName(asset.getAssetName());
        record.setCategoryId(asset.getCategoryId());
        record.setDepartmentId(asset.getDeptId());
        record.setDepreciationMethod(DEPRECIATION_METHOD);
        record.setOriginalValue(originalValue);
        record.setCurrentNetValue(newNetValue);
        record.setDepreciationAmount(periodDepreciation);
        record.setAccumulatedDepreciation(newAccumulatedDepreciation);
        record.setUsefulLife(usefulLife);
        record.setUsedMonths(usedMonths + (int)monthsInPeriod);
        record.setDepreciationPeriod(periodStartDate.toString() + "至" + periodEndDate.toString());
        record.setDepreciationMonth(periodStartDate.getYear() + "-" + String.format("%02d", periodStartDate.getMonthValue()));
        record.setNetValue(newNetValue);
        record.setStatus("COMPLETED");
        
        return record;
    }
    
    @Override
    public String getDepreciationMethod() {
        return DEPRECIATION_METHOD;
    }
    
    @Override
    public BigDecimal calculateNetValue(BigDecimal originalValue, BigDecimal accumulatedDepreciation) {
        BigDecimal netValue = originalValue.subtract(accumulatedDepreciation);
        return netValue.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : netValue;
    }
}