// Dark mode toggle
const themeToggle = document.getElementById('theme-toggle');
const prefersDarkScheme = window.matchMedia('(prefers-color-scheme: dark)');

// Get current theme from localStorage or system preference
const currentTheme = localStorage.getItem('theme') || (prefersDarkScheme.matches ? 'dark' : 'light');
document.documentElement.setAttribute('data-theme', currentTheme);

// Theme toggle handler
if (themeToggle) {
    themeToggle.addEventListener('click', function() {
        const currentTheme = document.documentElement.getAttribute('data-theme');
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        
        document.documentElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
    });
}

// WebSocket connection for live output
let stompClient = null;
let currentSubscription = null;

function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Disable debug output
    
    stompClient.connect({}, function(frame) {
        console.log('WebSocket connected');
    }, function(error) {
        console.error('WebSocket connection error:', error);
        setTimeout(connectWebSocket, 5000); // Retry after 5 seconds
    });
}

// Connect on page load
if (typeof SockJS !== 'undefined' && typeof Stomp !== 'undefined') {
    connectWebSocket();
}

// Execute task manually
function executeTask(taskId) {
    if (confirm(`Are you sure you want to execute task "${taskId}" now?`)) {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/execute/${taskId}`;
        
        // Add CSRF token if present
        const csrfToken = document.querySelector('input[name="_csrf"]');
        if (csrfToken) {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = '_csrf';
            input.value = csrfToken.value;
            form.appendChild(input);
        }
        
        document.body.appendChild(form);
        form.submit();
    }
}

// Live tail modal functions
function openLiveTail(taskId) {
    const modal = document.getElementById('liveTailModal');
    const output = document.getElementById('liveTailOutput');
    const taskIdSpan = document.getElementById('liveTailTaskId');
    
    taskIdSpan.textContent = taskId;
    output.innerHTML = 'Waiting for execution to start...\n';
    modal.style.display = 'block';
    
    // Start monitoring for new executions
    monitorTaskExecutions(taskId);
}

function closeLiveTail() {
    const modal = document.getElementById('liveTailModal');
    modal.style.display = 'none';
    
    // Unsubscribe from current execution
    if (currentSubscription) {
        currentSubscription.unsubscribe();
        currentSubscription = null;
    }
}

function monitorTaskExecutions(taskId) {
    // Poll for running executions
    const checkInterval = setInterval(() => {
        fetch(`/api/tasks/${taskId}/running-executions`)
            .then(response => response.json())
            .then(executions => {
                if (executions.length > 0) {
                    clearInterval(checkInterval);
                    subscribeToExecution(executions[0].id);
                }
            })
            .catch(error => console.error('Error checking executions:', error));
    }, 1000);
    
    // Stop checking after 30 seconds
    setTimeout(() => clearInterval(checkInterval), 30000);
}

function subscribeToExecution(executionId) {
    if (!stompClient || !stompClient.connected) {
        console.error('WebSocket not connected');
        return;
    }
    
    const output = document.getElementById('liveTailOutput');
    output.innerHTML = ''; // Clear previous content
    
    currentSubscription = stompClient.subscribe(`/topic/execution/${executionId}`, function(message) {
        const data = JSON.parse(message.body);
        const timestamp = new Date(data.timestamp).toLocaleTimeString();
        
        let line = `[${timestamp}] `;
        
        switch (data.type) {
            case 'START':
                line += `🚀 ${data.content}\n`;
                break;
            case 'STDOUT':
                line += `${data.content}\n`;
                break;
            case 'STDERR':
                line += `❌ ${data.content}\n`;
                break;
            case 'ERROR':
                line += `⚠️ ${data.content}\n`;
                break;
            case 'END':
                line += `✅ ${data.content}\n`;
                break;
            default:
                line += `${data.content}\n`;
        }
        
        output.innerHTML += line;
        output.scrollTop = output.scrollHeight; // Auto-scroll to bottom
    });
}

// View execution details
function viewExecutionDetails(executionId) {
    fetch(`/execution/${executionId}`)
        .then(response => response.json())
        .then(execution => {
            const modal = document.getElementById('executionModal');
            const details = document.getElementById('executionDetails');
            
            let html = `
                <div class="info-grid">
                    <div class="info-item">
                        <strong>Task ID:</strong> ${execution.task.taskId}
                    </div>
                    <div class="info-item">
                        <strong>Status:</strong> 
                        <span class="badge badge-${execution.status === 'SUCCESS' ? 'success' : 
                                                 execution.status === 'RUNNING' ? 'warning' : 'error'}">
                            ${execution.status}
                        </span>
                    </div>
                    <div class="info-item">
                        <strong>Start Time:</strong> ${new Date(execution.startTime).toLocaleString()}
                    </div>
                    <div class="info-item">
                        <strong>End Time:</strong> ${execution.endTime ? new Date(execution.endTime).toLocaleString() : 'N/A'}
                    </div>
                    <div class="info-item">
                        <strong>Duration:</strong> ${execution.executionTimeMs ? 
                            (execution.executionTimeMs < 1000 ? execution.executionTimeMs + 'ms' : 
                            (execution.executionTimeMs / 1000).toFixed(2) + 's') : 'N/A'}
                    </div>
                    <div class="info-item">
                        <strong>Exit Code:</strong> ${execution.exitCode || 'N/A'}
                    </div>
                    <div class="info-item">
                        <strong>Triggered By:</strong> ${execution.triggeredBy} 
                        ${execution.triggeredByUser ? `(${execution.triggeredByUser.username})` : ''}
                    </div>
                </div>
                
                ${execution.errorMessage ? `
                    <div class="mt-3">
                        <h4>Error Message:</h4>
                        <div class="alert alert-error">${escapeHtml(execution.errorMessage)}</div>
                    </div>
                ` : ''}
                
                <div class="mt-3">
                    <h4>Standard Output:</h4>
                    <div class="console-output">${escapeHtml(execution.stdout || 'No output')}</div>
                </div>
                
                ${execution.stderr ? `
                    <div class="mt-3">
                        <h4>Error Output:</h4>
                        <div class="console-output" style="color: #ff6b6b;">${escapeHtml(execution.stderr)}</div>
                    </div>
                ` : ''}
            `;
            
            details.innerHTML = html;
            modal.style.display = 'block';
        })
        .catch(error => {
            console.error('Error fetching execution details:', error);
            alert('Failed to fetch execution details');
        });
}

function closeExecutionModal() {
    const modal = document.getElementById('executionModal');
    modal.style.display = 'none';
}

// Utility function to escape HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Close modals when clicking outside
window.onclick = function(event) {
    const liveTailModal = document.getElementById('liveTailModal');
    const executionModal = document.getElementById('executionModal');
    
    if (event.target === liveTailModal) {
        closeLiveTail();
    } else if (event.target === executionModal) {
        closeExecutionModal();
    }
}

// Auto-refresh recent executions every 30 seconds
if (window.location.pathname === '/') {
    setInterval(() => {
        if (!document.hidden) {
            location.reload();
        }
    }, 30000);
}
