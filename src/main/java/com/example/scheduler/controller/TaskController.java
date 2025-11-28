package com.example.scheduler.controller;

import com.example.scheduler.model.AuditLog;
import com.example.scheduler.model.TaskDefinition;
import com.example.scheduler.model.TaskExecution;
import com.example.scheduler.model.User;
import com.example.scheduler.repository.AuditLogRepository;
import com.example.scheduler.repository.TaskDefinitionRepository;
import com.example.scheduler.repository.TaskExecutionRepository;
import com.example.scheduler.service.AuditService;
import com.example.scheduler.service.DynamicTaskSchedulerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TaskController {
    
    private final DynamicTaskSchedulerService schedulerService;
    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    
    @GetMapping("/")
    public String index(Model model, @AuthenticationPrincipal User currentUser) {
        List<TaskDefinition> tasks = taskDefinitionRepository.findAll();
        Map<String, Boolean> scheduledStatus = schedulerService.getScheduledTasksStatus();
        
        // Add scheduled status to tasks
        tasks.forEach(task -> task.setScheduled(scheduledStatus.getOrDefault(task.getTaskId(), false)));
        
        model.addAttribute("tasks", tasks);
        model.addAttribute("newTask", new TaskDefinition());
        model.addAttribute("currentUser", currentUser);
        
        // Recent executions
        Page<TaskExecution> recentExecutions = taskExecutionRepository.findAllByOrderByStartTimeDesc(
            PageRequest.of(0, 10)
        );
        model.addAttribute("recentExecutions", recentExecutions.getContent());
        
        return "index";
    }
    
    @PostMapping("/schedule")
    public String scheduleTask(@Valid @ModelAttribute("newTask") TaskDefinition task,
                              BindingResult result,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Validation failed: " + result.getAllErrors());
            return "redirect:/";
        }
        
        try {
            schedulerService.scheduleTask(task, currentUser);
            redirectAttributes.addFlashAttribute("success", "Task scheduled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to schedule task: " + e.getMessage());
        }
        
        return "redirect:/";
    }
    
    @PostMapping("/cancel/{taskId}")
    public String cancelTask(@PathVariable String taskId,
                            @AuthenticationPrincipal User currentUser,
                            RedirectAttributes redirectAttributes) {
        try {
            schedulerService.cancelTask(taskId, currentUser);
            redirectAttributes.addFlashAttribute("success", "Task cancelled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cancel task: " + e.getMessage());
        }
        
        return "redirect:/";
    }
    
    @PostMapping("/execute/{taskId}")
    public String executeTaskManually(@PathVariable String taskId,
                                     @AuthenticationPrincipal User currentUser,
                                     RedirectAttributes redirectAttributes) {
        try {
            TaskDefinition task = taskDefinitionRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            
            schedulerService.executeTask(task, "MANUAL", currentUser, null);
            auditService.logAction("MANUAL_EXECUTION", taskId, currentUser, "Task executed manually");
            redirectAttributes.addFlashAttribute("success", "Task execution started!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to execute task: " + e.getMessage());
        }
        
        return "redirect:/";
    }
    
    @GetMapping("/task/{taskId}")
    public String taskDetails(@PathVariable String taskId, Model model) {
        TaskDefinition task = taskDefinitionRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        Page<TaskExecution> executions = taskExecutionRepository.findByTaskOrderByStartTimeDesc(
            task, PageRequest.of(0, 20)
        );
        
        Page<AuditLog> auditLogs = auditLogRepository.findByTaskIdOrderByTimestampDesc(
            taskId, PageRequest.of(0, 20)
        );
        
        model.addAttribute("task", task);
        model.addAttribute("executions", executions);
        model.addAttribute("auditLogs", auditLogs);
        model.addAttribute("allTasks", taskDefinitionRepository.findAll()); // For dependencies
        
        return "task-details";
    }
    
    @GetMapping("/execution/{executionId}")
    @ResponseBody
    public TaskExecution getExecution(@PathVariable Long executionId) {
        return taskExecutionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));
    }
    
    @GetMapping("/audit")
    public String auditLogs(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "50") int size,
                           Model model) {
        Page<AuditLog> auditPage = auditLogRepository.findAllByOrderByTimestampDesc(
            PageRequest.of(page, size)
        );
        
        model.addAttribute("auditLogs", auditPage);
        return "audit-logs";
    }
    
    @PostMapping("/task/{taskId}/dependencies")
    public String updateDependencies(@PathVariable String taskId,
                                   @RequestParam(required = false) List<String> dependentTaskIds,
                                   @AuthenticationPrincipal User currentUser,
                                   RedirectAttributes redirectAttributes) {
        try {
            TaskDefinition task = taskDefinitionRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            
            task.getDependentTasks().clear();
            
            if (dependentTaskIds != null) {
                for (String depId : dependentTaskIds) {
                    if (!depId.equals(taskId)) { // Prevent self-dependency
                        taskDefinitionRepository.findById(depId).ifPresent(depTask -> {
                            task.getDependentTasks().add(depTask);
                        });
                    }
                }
            }
            
            task.setModifiedBy(currentUser);
            taskDefinitionRepository.save(task);
            
            auditService.logAction("DEPENDENCIES_UPDATED", taskId, currentUser, 
                "Dependencies updated: " + (dependentTaskIds != null ? String.join(", ", dependentTaskIds) : "none"));
            
            redirectAttributes.addFlashAttribute("success", "Dependencies updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update dependencies: " + e.getMessage());
        }
        
        return "redirect:/task/" + taskId;
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
