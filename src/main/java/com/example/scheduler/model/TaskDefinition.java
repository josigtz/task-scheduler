package com.example.scheduler.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "task_definitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDefinition {
    
    @Id
    private String taskId;
    
    @NotBlank(message = "Script path is required")
    private String scriptPath;
    
    @NotBlank(message = "Cron expression is required")
    @Pattern(regexp = "^(\\S+\\s+){5}\\S+$", message = "Invalid cron expression format")
    private String cronExpression;
    
    private String description;
    
    private boolean enabled = true;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime lastModified = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;
    
    // Dependencies - tasks that should run after this task completes successfully
    @ManyToMany
    @JoinTable(
        name = "task_dependencies",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "dependent_task_id")
    )
    private List<TaskDefinition> dependentTasks = new ArrayList<>();
    
    // Environment variables for the script
    @ElementCollection
    @CollectionTable(name = "task_env_variables")
    @MapKeyColumn(name = "env_key")
    @Column(name = "env_value")
    private Map<String, String> environmentVariables = new HashMap<>();
    
    private Integer maxRetries = 0;
    
    private Long timeoutSeconds = 3600L; // 1 hour default
    
    @Transient
    private boolean scheduled = false;
}
