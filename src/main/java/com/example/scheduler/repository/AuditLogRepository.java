package com.example.scheduler.repository;

import com.example.scheduler.model.AuditLog;
import com.example.scheduler.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    Page<AuditLog> findByTaskIdOrderByTimestampDesc(String taskId, Pageable pageable);
    
    Page<AuditLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);
    
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
    
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
