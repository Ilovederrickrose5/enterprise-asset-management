package com.enterprise.asset.enterpriseassetmanagement.repository;

import com.enterprise.asset.enterpriseassetmanagement.entity.SysLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysLogRepository extends JpaRepository<SysLog, Long> {
    
    @Query("SELECT s FROM SysLog s ORDER BY s.createTime DESC LIMIT ?1")
    List<SysLog> findRecentLogs(int limit);
}
