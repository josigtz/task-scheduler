package com.example.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String action; // SCHEDULED, CANCELLED, MODIFIED, EXECUTED, etc.
    
    @Column(nullable = false)
    private String taskId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(length = 1000)
    private String details;
    
    private String ipAddress;
    
    private String userAgent;
    
    // Additional context
    @Column(columnDefinition = "TEXT")
    private String additionalData;
}
