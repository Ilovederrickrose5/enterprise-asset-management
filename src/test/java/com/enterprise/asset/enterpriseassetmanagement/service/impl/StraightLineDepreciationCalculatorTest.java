package com.enterprise.asset.enterpriseassetmanagement.service.impl;

import com.enterprise.asset.enterpriseassetmanagement.entity.Asset;
import com.enterprise.asset.enterpriseassetmanagement.entity.DepreciationRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class StraightLineDepreciationCalculatorTest {
    
    private StraightLineDepreciationCalculator calculator;
    private Asset testAsset;
    
    @BeforeEach
    void setUp() {
        calculator = new StraightLineDepreciationCalculator();
        testAsset = new Asset();
        testAsset.setId(1L);
        testAsset.setAssetNo("TEST001");
        testAsset.setAssetName("测试资产");
        testAsset.setCategoryId(1L);
        testAsset.setDeptId(1L);
        testAsset.setOriginalValue(new BigDecimal("12000"));
        testAsset.setUsefulLife(60);
    }
    
    @Test
    void testCalculateDepreciation_NormalCase() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        
        DepreciationRecord record = calculator.calculateDepreciation(
            testAsset, startDate, endDate, BigDecimal.ZERO, 0
        );
        
        assertNotNull(record);
        assertEquals("STRAIGHT_LINE", record.getDepreciationMethod());
        assertEquals(testAsset.getAssetNo(), record.getAssetNo());
        assertEquals(testAsset.getAssetName(), record.getAssetName());
        assertEquals(testAsset.getOriginalValue(), record.getOriginalValue());
        
        BigDecimal expectedMonthlyDepreciation = new BigDecimal("12000").divide(new BigDecimal(60), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal expectedDepreciation = expectedMonthlyDepreciation.multiply(new BigDecimal(1));
        
        assertEquals(0, expectedDepreciation.compareTo(record.getDepreciationAmount()));
        assertEquals(0, record.getAccumulatedDepreciation().compareTo(expectedDepreciation));
        assertEquals(0, record.getCurrentNetValue().compareTo(new BigDecimal("12000").subtract(expectedDepreciation)));
        assertEquals(1, record.getUsedMonths());
        assertEquals("COMPLETED", record.getStatus());
    }
    
    @Test
    void testCalculateDepreciation_MultipleMonths() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);
        
        BigDecimal accumulatedDepreciation = new BigDecimal("400");
        Integer usedMonths = 10;
        
        DepreciationRecord record = calculator.calculateDepreciation(
            testAsset, startDate, endDate, accumulatedDepreciation, usedMonths
        );
        
        assertNotNull(record);
        assertEquals(13, record.getUsedMonths());
        
        BigDecimal expectedMonthlyDepreciation = new BigDecimal("12000").divide(new BigDecimal(60), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal expectedPeriodDepreciation = expectedMonthlyDepreciation.multiply(new BigDecimal(3));
        BigDecimal expectedAccumulated = accumulatedDepreciation.add(expectedPeriodDepreciation);
        
        assertEquals(0, expectedPeriodDepreciation.compareTo(record.getDepreciationAmount()));
        assertEquals(0, expectedAccumulated.compareTo(record.getAccumulatedDepreciation()));
    }
    
    @Test
    void testCalculateDepreciation_ZeroOriginalValue() {
        testAsset.setOriginalValue(BigDecimal.ZERO);
        
        DepreciationRecord record = calculator.calculateDepreciation(
            testAsset, LocalDate.now(), LocalDate.now(), BigDecimal.ZERO, 0
        );
        
        assertNotNull(record);
        assertEquals(0, record.getDepreciationAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, record.getCurrentNetValue().compareTo(BigDecimal.ZERO));
    }
    
    @Test
    void testCalculateDepreciation_ZeroUsefulLife() {
        testAsset.setUsefulLife(0);
        
        DepreciationRecord record = calculator.calculateDepreciation(
            testAsset, LocalDate.now(), LocalDate.now(), BigDecimal.ZERO, 0
        );
        
        assertNotNull(record);
        assertEquals(0, record.getDepreciationAmount().compareTo(BigDecimal.ZERO));
    }
    
    @Test
    void testCalculateDepreciation_FullyDepreciated() {
        BigDecimal accumulatedDepreciation = new BigDecimal("12000");
        Integer usedMonths = 60;
        
        DepreciationRecord record = calculator.calculateDepreciation(
            testAsset, LocalDate.now(), LocalDate.now(), accumulatedDepreciation, usedMonths
        );
        
        assertNotNull(record);
        assertEquals(0, record.getCurrentNetValue().compareTo(BigDecimal.ZERO));
        assertEquals(0, record.getAccumulatedDepreciation().compareTo(new BigDecimal("12000")));
    }
    
    @Test
    void testCalculateNetValue() {
        BigDecimal originalValue = new BigDecimal("10000");
        BigDecimal accumulatedDepreciation = new BigDecimal("3000");
        
        BigDecimal netValue = calculator.calculateNetValue(originalValue, accumulatedDepreciation);
        
        assertEquals(0, netValue.compareTo(new BigDecimal("7000")));
    }
    
    @Test
    void testCalculateNetValue_NegativeResult() {
        BigDecimal originalValue = new BigDecimal("10000");
        BigDecimal accumulatedDepreciation = new BigDecimal("12000");
        
        BigDecimal netValue = calculator.calculateNetValue(originalValue, accumulatedDepreciation);
        
        assertEquals(0, netValue.compareTo(BigDecimal.ZERO));
    }
    
    @Test
    void testGetDepreciationMethod() {
        assertEquals("STRAIGHT_LINE", calculator.getDepreciationMethod());
    }
}