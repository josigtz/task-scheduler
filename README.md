# 🕒 Enhanced On-Demand Cron Shell Scheduler

A feature-rich Spring Boot application that provides a secure Web UI to schedule, manage, and execute local shell scripts dynamically using Cron expressions. Now with authentication, audit logging, execution history, task dependencies, dark mode, and live output streaming!

## 📖 Overview

This enhanced version builds upon the original scheduler with enterprise-grade features for better control, security, and monitoring of scheduled tasks.

### ✨ New Features

* **🔐 User Authentication**: Basic authentication to restrict access
* **📝 Audit Logging**: Complete audit trail of who did what and when
* **📊 Task History**: Detailed execution history with output storage
* **🔗 Task Dependencies**: Chain tasks to run after successful completion
* **🌓 Dark Mode**: Toggle between light and dark themes
* **📡 Live Output**: Real-time streaming of script output via WebSockets
* **💾 Persistent Storage**: H2 database for data persistence

### 🔧 Original Features

* Dynamic Scheduling: Add or cancel tasks at runtime
* Cron Precision: Full support for standard Cron expressions
* Shell Integration: Executes native shell scripts (`.sh`, `.bat`)
* Validation: Ensures script files exist before scheduling

## ⚠️ Security Warning

**CRITICAL**: This application allows the execution of arbitrary shell scripts on the host server.
* DO NOT expose this application to the public internet
* Change the default admin password immediately
* Run with minimal required privileges
* Use in secure, controlled environments only

## 🚀 Getting Started

### Prerequisites

* Java 17 or higher
* Maven (or use the included `mvnw` wrapper)
* Operating System: Linux/macOS (for `.sh`) or Windows (for `.bat`/`.cmd`)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/cron-shell-scheduler-enhanced.git
cd cron-shell-scheduler-enhanced
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

4. Access the application at `http://localhost:8080`

5. Login with default credentials:
   * Username: `admin`
   * Password: `admin123`
   
   **⚠️ IMPORTANT: Change the default password immediately!**

## 📝 Usage Guide

### 1. Create Test Scripts

Linux/macOS example (`/tmp/backup.sh`):
```bash
#!/bin/bash
echo "Starting backup at $(date)"
# Your backup logic here
echo "Backup completed successfully"
```

Make it executable:
```bash
chmod +x /tmp/backup.sh
```

Windows example (`C:\Scripts\cleanup.bat`):
```batch
@echo off
echo Starting cleanup at %DATE% %TIME%
REM Your cleanup logic here
echo Cleanup completed successfully
```

### 2. Schedule Tasks

1. Navigate to the dashboard
2. Fill in the "Schedule New Task" form:
   * **Task ID**: Unique identifier (e.g., `daily-backup`)
   * **Script Path**: Full path to your script
   * **Cron Expression**: When to run (e.g., `0 0 2 * * *` for 2 AM daily)
   * **Description**: Optional description
3. Click "Schedule Task"

### 3. Manage Tasks

* **View Details**: Click on any task ID to see detailed information
* **Manual Execution**: Click ▶️ to run a task immediately
* **Live Output**: Click 📡 to watch real-time output
* **Cancel Task**: Click ❌ to stop and disable a task

### 4. Set Up Dependencies

1. Go to a task's detail page
2. In the "Task Dependencies" section, select tasks to trigger upon success
3. Click "Update Dependencies"

### 5. Monitor Activity

* **Dashboard**: View all tasks and recent executions
* **Task Details**: See execution history and audit logs per task
* **Audit Logs**: Review all system activity with user attribution

## ⏰ Cron Expression Reference

| Expression | Description |
|------------|-------------|
| `0 0 * * * *` | Every hour |
| `0 0 0 * * *` | Daily at midnight |
| `0 0 9 * * MON-FRI` | Weekdays at 9 AM |
| `0 */15 * * * *` | Every 15 minutes |
| `0 0 0 1 * *` | First day of month |

## 🏗️ Architecture

### Technology Stack
* Spring Boot 3.2
* Spring Security (Basic Auth)
* Spring Data JPA
* H2 Database (file-based)
* WebSockets (STOMP)
* Thymeleaf Templates

### Key Components
* **DynamicTaskSchedulerService**: Core scheduling and execution engine
* **AuditService**: Tracks all user actions
* **WebSocket Integration**: Real-time output streaming
* **Task Dependencies**: Workflow orchestration

## 🔒 Security Considerations

1. **Change Default Password**: First priority after installation
2. **Network Security**: Use firewall rules to restrict access
3. **Script Permissions**: Limit what scripts can do
4. **User Accounts**: Create individual accounts for auditing
5. **Regular Backups**: Backup the H2 database regularly

## 🐛 Troubleshooting

### Common Issues

**Task not executing?**
- Check if the script file exists and is executable
- Verify the cron expression is valid
- Check application logs for errors

**Can't see live output?**
- Ensure WebSocket connection is established
- Check browser console for errors
- Try refreshing the page

**Database issues?**
- H2 database is stored in `./data/scheduler.mv.db`
- Delete this file to reset (loses all data)

## 🤝 Contributing

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

## 🙏 Acknowledgments

* Spring Boot team for the excellent framework
* Contributors and users providing feedback
* The open-source community
