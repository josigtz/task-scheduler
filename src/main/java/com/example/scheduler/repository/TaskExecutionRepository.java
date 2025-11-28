package com.example.scheduler.repository;

import com.example.scheduler.model.TaskDefinition;
import com.example.scheduler.model.TaskExecution;
import com.example.scheduler.model.TaskExecution.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long> {
    
    Page<TaskExecution> findByTaskOrderByStartTimeDesc(TaskDefinition task, Pageable pageable);
    
    List<TaskExecution> findByTaskAndStatus(TaskDefinition task, ExecutionStatus status);
    
    @Query("SELECT te FROM TaskExecution te WHERE te.task.taskId = :taskId AND te.status = 'RUNNING'")
    List<TaskExecution> findRunningExecutions(@Param("taskId") String taskId);
    
    Optional<TaskExecution> findTopByTaskOrderByStartTimeDesc(TaskDefinition task);
    
    @Query("SELECT COUNT(te) FROM TaskExecution te WHERE te.task = :task AND te.status = :status " +
           "AND te.startTime >= :since")
    long countByTaskAndStatusSince(@Param("task") TaskDefinition task, 
                                   @Param("status") ExecutionStatus status, 
                                   @Param("since") LocalDateTime since);
    
    Page<TaskExecution> findAllByOrderByStartTimeDesc(Pageable pageable);
}
