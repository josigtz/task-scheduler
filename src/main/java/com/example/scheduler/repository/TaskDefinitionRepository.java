package com.example.scheduler.repository;

import com.example.scheduler.model.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, String> {
    
    List<TaskDefinition> findByEnabledTrue();
    
    @Query("SELECT t FROM TaskDefinition t LEFT JOIN FETCH t.dependentTasks WHERE t.enabled = true")
    List<TaskDefinition> findAllActiveWithDependencies();
    
    @Query("SELECT t FROM TaskDefinition t WHERE :task MEMBER OF t.dependentTasks")
    List<TaskDefinition> findTasksDependentOn(TaskDefinition task);
}
