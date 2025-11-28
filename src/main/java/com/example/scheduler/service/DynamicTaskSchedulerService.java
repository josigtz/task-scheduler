package com.example.scheduler.service;

import com.example.scheduler.model.TaskDefinition;
import com.example.scheduler.model.TaskExecution;
import com.example.scheduler.model.TaskExecution.ExecutionStatus;
import com.example.scheduler.model.User;
import com.example.scheduler.repository.TaskDefinitionRepository;
import com.example.scheduler.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicTaskSchedulerService {
    
    private final TaskScheduler taskScheduler;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final AuditService auditService;
    private final SimpMessagingTemplate messagingTemplate;
    
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    @Transactional
    public void scheduleTask(TaskDefinition taskDef, User user) {
        try {
            if (scheduledTasks.containsKey(taskDef.getTaskId())) {
                ScheduledFuture<?> existing = scheduledTasks.get(taskDef.getTaskId());
                if (existing != null && (existing.isCancelled() || existing.isDone())) {
                    scheduledTasks.remove(taskDef.getTaskId());
                    log.info("Removed stale schedule for task '{}' before rescheduling", taskDef.getTaskId());
                } else {
                    throw new IllegalStateException("Task already scheduled: " + taskDef.getTaskId());
                }
            }
            
            // Validate script exists
            File scriptFile = new File(taskDef.getScriptPath());
            if (!scriptFile.exists() || !scriptFile.isFile()) {
                throw new IllegalArgumentException("Script file not found: " + taskDef.getScriptPath());
            }
            
            // Save task definition
            taskDef.setCreatedBy(user);
            taskDef.setModifiedBy(user);
            taskDefinitionRepository.save(taskDef);
            
            // Schedule the task
            ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(
                () -> executeTask(taskDef, "SCHEDULED", null, null),
                new CronTrigger(taskDef.getCronExpression())
            );
            
            scheduledTasks.put(taskDef.getTaskId(), scheduledFuture);
            auditService.logAction("SCHEDULED", taskDef.getTaskId(), user, "Task scheduled with cron: " + taskDef.getCronExpression());
            
            log.info("Task '{}' scheduled successfully", taskDef.getTaskId());
        } catch (Exception e) {
            log.error("Failed to schedule task: {}", taskDef.getTaskId(), e);
            throw e;
        }
    }
    
    @Transactional
    public void cancelTask(String taskId, User user) {
        ScheduledFuture<?> scheduledFuture = scheduledTasks.remove(taskId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            
            // Disable the task in database
            taskDefinitionRepository.findById(taskId).ifPresent(task -> {
                task.setEnabled(false);
                task.setModifiedBy(user);
                task.setLastModified(LocalDateTime.now());
                taskDefinitionRepository.save(task);
            });
            
            auditService.logAction("CANCELLED", taskId, user, "Task cancelled and disabled");
            log.info("Task '{}' cancelled", taskId);
        }
    }
    
    @Transactional
    public TaskExecution executeTask(TaskDefinition taskDef, String triggeredBy, User triggeredByUser, TaskExecution parentExecution) {
        TaskExecution execution = TaskExecution.builder()
                .task(taskDef)
                .startTime(LocalDateTime.now())
                .status(ExecutionStatus.RUNNING)
                .triggeredBy(triggeredBy)
                .triggeredByUser(triggeredByUser)
                .parentExecution(parentExecution)
                .build();
        
        execution = taskExecutionRepository.save(execution);
        
        String executionTopic = "/topic/execution/" + execution.getId();
        
        try {
            log.info("▶️ Starting task '{}', execution: {}", taskDef.getTaskId(), execution.getId());
            messagingTemplate.convertAndSend(executionTopic, createOutputMessage("START", "Starting task execution..."));
            
            ProcessBuilder processBuilder = new ProcessBuilder();
            
            // Determine command based on OS
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                processBuilder.command("cmd.exe", "/c", taskDef.getScriptPath());
            } else {
                processBuilder.command("/bin/bash", "-c", taskDef.getScriptPath());
            }
            
            // Set environment variables
            Map<String, String> env = processBuilder.environment();
            if (taskDef.getEnvironmentVariables() != null) {
                env.putAll(taskDef.getEnvironmentVariables());
            }
            
            processBuilder.redirectErrorStream(false);
            Process process = processBuilder.start();
            
            // Start output readers
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            
            Thread stdoutReader = new Thread(() -> readStream(process.getInputStream(), stdout, executionTopic, "STDOUT"));
            Thread stderrReader = new Thread(() -> readStream(process.getErrorStream(), stderr, executionTopic, "STDERR"));
            
            stdoutReader.start();
            stderrReader.start();
            
            // Wait for completion with timeout
            boolean completed = process.waitFor(taskDef.getTimeoutSeconds(), TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                execution.setStatus(ExecutionStatus.TIMEOUT);
                messagingTemplate.convertAndSend(executionTopic, createOutputMessage("ERROR", "Task timeout after " + taskDef.getTimeoutSeconds() + " seconds"));
            } else {
                int exitCode = process.exitValue();
                execution.setExitCode(exitCode);
                execution.setStatus(exitCode == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED);
            }
            
            // Wait for readers to finish
            stdoutReader.join(5000);
            stderrReader.join(5000);
            
            execution.setStdout(stdout.toString());
            execution.setStderr(stderr.toString());
            
        } catch (Exception e) {
            log.error("Task execution failed: {}", taskDef.getTaskId(), e);
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            messagingTemplate.convertAndSend(executionTopic, createOutputMessage("ERROR", "Execution failed: " + e.getMessage()));
        } finally {
            execution.setEndTime(LocalDateTime.now());
            execution.setExecutionTimeMs(
                java.time.Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis()
            );
            execution = taskExecutionRepository.save(execution);
            
            messagingTemplate.convertAndSend(executionTopic, createOutputMessage("END", "Task completed with status: " + execution.getStatus()));
            
            // Trigger dependent tasks if successful
            if (execution.getStatus() == ExecutionStatus.SUCCESS) {
                triggerDependentTasks(taskDef, execution);
            }
            
            log.info("{} Task '{}' finished with status: {}", 
                    execution.getStatus() == ExecutionStatus.SUCCESS ? "✅" : "❌",
                    taskDef.getTaskId(), 
                    execution.getStatus());
        }
        
        return execution;
    }
    
    private void readStream(java.io.InputStream inputStream, StringBuilder output, String topic, String type) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                messagingTemplate.convertAndSend(topic, createOutputMessage(type, line));
            }
        } catch (Exception e) {
            log.error("Error reading process stream", e);
        }
    }
    
    private Map<String, Object> createOutputMessage(String type, String content) {
        return Map.of(
            "type", type,
            "content", content,
            "timestamp", LocalDateTime.now().toString()
        );
    }
    
    private void triggerDependentTasks(TaskDefinition parentTask, TaskExecution parentExecution) {
        List<TaskDefinition> dependentTasks = parentTask.getDependentTasks();
        if (dependentTasks != null && !dependentTasks.isEmpty()) {
            for (TaskDefinition dependentTask : dependentTasks) {
                if (dependentTask.isEnabled()) {
                    log.info("Triggering dependent task: {} (parent: {})", dependentTask.getTaskId(), parentTask.getTaskId());
                    User triggeredUser = parentExecution != null ? parentExecution.getTriggeredByUser() : null;
                    auditService.logAction(
                        "DEPENDENCY_TRIGGERED",
                        dependentTask.getTaskId(),
                        triggeredUser,
                        "Triggered by successful completion of " + parentTask.getTaskId()
                    );
                    taskScheduler.execute(() -> executeTask(dependentTask, "DEPENDENCY", triggeredUser, parentExecution));
                }
            }
        }
    }
    
    @Transactional
    public void initializeScheduledTasks() {
        List<TaskDefinition> activeTasks = taskDefinitionRepository.findByEnabledTrue();
        for (TaskDefinition task : activeTasks) {
            try {
                ScheduledFuture<?> existingSchedule = scheduledTasks.get(task.getTaskId());
                if (existingSchedule != null) {
                    if (existingSchedule.isCancelled() || existingSchedule.isDone()) {
                        scheduledTasks.remove(task.getTaskId());
                        log.info("Task '{}' had stale schedule; re-initializing", task.getTaskId());
                    } else {
                        log.warn("Task '{}' is already scheduled; skipping initialization", task.getTaskId());
                        continue;
                    }
                }

                ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(
                    () -> executeTask(task, "SCHEDULED", null, null),
                    new CronTrigger(task.getCronExpression())
                );

                scheduledTasks.put(task.getTaskId(), scheduledFuture);
                log.info("Initialized scheduled task: {}", task.getTaskId());
            } catch (Exception e) {
                log.error("Failed to initialize task: {}", task.getTaskId(), e);
            }
        }
    }
    
    public Map<String, Boolean> getScheduledTasksStatus() {
        Map<String, Boolean> status = new ConcurrentHashMap<>();
        for (TaskDefinition task : taskDefinitionRepository.findAll()) {
            status.put(task.getTaskId(), scheduledTasks.containsKey(task.getTaskId()));
        }
        return status;
    }
}
