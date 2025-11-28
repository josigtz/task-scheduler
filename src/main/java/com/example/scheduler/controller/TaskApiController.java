package com.example.scheduler.controller;

import com.example.scheduler.model.TaskExecution;
import com.example.scheduler.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskApiController {
    
    private final TaskExecutionRepository taskExecutionRepository;
    
    @GetMapping("/tasks/{taskId}/running-executions")
    public List<TaskExecution> getRunningExecutions(@PathVariable String taskId) {
        return taskExecutionRepository.findRunningExecutions(taskId);
    }
}
