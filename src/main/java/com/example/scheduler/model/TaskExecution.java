package com.example.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskDefinition task;
    
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status = ExecutionStatus.RUNNING;
    
    private Integer exitCode;
    
    @Column(columnDefinition = "TEXT")
    private String stdout;
    
    @Column(columnDefinition = "TEXT")
    private String stderr;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    private Long executionTimeMs;
    
    @Column(nullable = false)
    private String triggeredBy = "SCHEDULED"; // SCHEDULED, MANUAL, DEPENDENCY
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "triggered_by_user")
    private User triggeredByUser;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_execution_id")
    private TaskExecution parentExecution; // For dependency-triggered executions
    
    public enum ExecutionStatus {
        RUNNING,
        SUCCESS,
        FAILED,
        TIMEOUT,
        CANCELLED
    }
}
