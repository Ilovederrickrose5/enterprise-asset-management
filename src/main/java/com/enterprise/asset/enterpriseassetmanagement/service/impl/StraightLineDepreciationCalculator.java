package com.enterprise.asset.enterpriseassetmanagement.service.impl;

import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.DepreciationRecord;
import com.enterprise.asset.enterpriseassetmanagement.service.DepreciationCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StraightLineDepreciationCalculator implements DepreciationCalculator {

    private static final String DEPRECIATION_METHOD = "STRAIGHT_LINE";
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
        Integer usefulLifeYears = asset.getUsefulLife() != null ? asset.getUsefulLife() : 0;

        if (usefulLifeYears <= 0 || originalValue.compareTo(BigDecimal.ZERO) <= 0) {
            record.setDepreciationAmount(BigDecimal.ZERO);
            record.setCurrentNetValue(originalValue);
            record.setAccumulatedDepreciation(accumulatedDepreciation);
            return record;
        }

        Integer usefulLifeMonths = usefulLifeYears * 12;

        BigDecimal monthlyDepreciation = originalValue.divide(
                new BigDecimal(usefulLifeMonths),
                DECIMAL_PLACES,
                ROUNDING_MODE);

        long monthsInPeriod = ChronoUnit.MONTHS.between(periodStartDate, periodEndDate) + 1;

        BigDecimal periodDepreciation = monthlyDepreciation.multiply(new BigDecimal(monthsInPeriod))
                .setScale(DECIMAL_PLACES, ROUNDING_MODE);

        BigDecimal newAccumulatedDepreciation = accumulatedDepreciation.add(periodDepreciation);
        BigDecimal currentNetValue = originalValue.subtract(newAccumulatedDepreciation);

        if (currentNetValue.compareTo(BigDecimal.ZERO) < 0) {
            currentNetValue = BigDecimal.ZERO;
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
        record.setCurrentNetValue(currentNetValue);
        record.setDepreciationAmount(periodDepreciation);
        record.setAccumulatedDepreciation(newAccumulatedDepreciation);
        record.setUsefulLife(usefulLifeMonths);
        record.setUsedMonths(usedMonths + (int) monthsInPeriod);
        record.setDepreciationPeriod(periodStartDate.toString() + "至" + periodEndDate.toString());
        record.setDepreciationMonth(
                periodStartDate.getYear() + "-" + String.format("%02d", periodStartDate.getMonthValue()));
        record.setNetValue(currentNetValue);
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