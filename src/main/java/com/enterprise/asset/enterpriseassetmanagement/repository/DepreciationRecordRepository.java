package com.enterprise.asset.enterpriseassetmanagement.repository;

import com.enterprise.asset.enterpriseassetmanagement.entity.DepreciationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepreciationRecordRepository extends JpaRepository<DepreciationRecord, Long> {

        List<DepreciationRecord> findByAssetId(Long assetId);

        List<DepreciationRecord> findByAssetIdOrderByCreateTimeDesc(Long assetId);

        Optional<DepreciationRecord> findTopByAssetIdOrderByCreateTimeDesc(Long assetId);

        List<DepreciationRecord> findByDepreciationPeriodAndCreateTimeBetween(
                        String depreciationPeriod,
                        LocalDateTime startDate,
                        LocalDateTime endDate);

        List<DepreciationRecord> findByCreateTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

        List<DepreciationRecord> findByCategoryId(Long categoryId);

        List<DepreciationRecord> findByDepartmentId(Long departmentId);

        @Query("SELECT dr FROM DepreciationRecord dr WHERE dr.createTime = :date AND dr.status = :status")
        List<DepreciationRecord> findByCreateTimeAndStatus(
                        @Param("date") LocalDateTime date,
                        @Param("status") String status);

        @Query("SELECT SUM(dr.depreciationAmount) FROM DepreciationRecord dr WHERE dr.assetId = :assetId")
        java.math.BigDecimal getTotalDepreciationByAssetId(@Param("assetId") Long assetId);

        @Query("SELECT COUNT(dr) FROM DepreciationRecord dr WHERE dr.assetId = :assetId")
        Long countByAssetId(@Param("assetId") Long assetId);

        @Query("SELECT dr FROM DepreciationRecord dr WHERE dr.depreciationPeriod = :period ORDER BY dr.createTime DESC")
        List<DepreciationRecord> findByDepreciationPeriod(@Param("period") String period);

        @Query("SELECT dr FROM DepreciationRecord dr WHERE dr.assetId IN :assetIds AND dr.createTime BETWEEN :startDate AND :endDate")
        List<DepreciationRecord> findByAssetIdsAndCreateTimeBetween(
                        @Param("assetIds") List<Long> assetIds,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT dr FROM DepreciationRecord dr WHERE dr.id IN (SELECT MAX(dr2.id) FROM DepreciationRecord dr2 WHERE dr2.assetId IN :assetIds GROUP BY dr2.assetId)")
        List<DepreciationRecord> findLatestByAssetIds(@Param("assetIds") List<Long> assetIds);

        void deleteByAssetIdAndDepreciationMonth(Long assetId, String depreciationMonth);
}