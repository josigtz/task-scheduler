# Migration Guide: Upgrading to Enhanced Cron Shell Scheduler

## Overview

This guide helps you migrate from the original Cron Shell Scheduler to the enhanced version with authentication, audit logging, and other new features.

## Key Changes

### 1. Database Addition
- The enhanced version uses H2 database for persistence
- Database file is created at `./data/scheduler.mv.db`
- All task definitions and history are now persisted

### 2. Authentication Required
- Basic authentication is now mandatory
- Default credentials: `admin / admin123`
- Change the password immediately after first login

### 3. Package Structure Changes
```
Original:
├── controller/
│   └── TaskController.java
├── model/
│   └── TaskDefinition.java
└── service/
    └── DynamicTaskSchedulerService.java

Enhanced:
├── controller/
│   ├── TaskController.java
│   └── TaskApiController.java
├── model/
│   ├── TaskDefinition.java (enhanced)
│   ├── User.java (new)
│   ├── AuditLog.java (new)
│   └── TaskExecution.java (new)
├── repository/ (new)
│   ├── UserRepository.java
│   ├── TaskDefinitionRepository.java
│   ├── AuditLogRepository.java
│   └── TaskExecutionRepository.java
├── service/
│   ├── DynamicTaskSchedulerService.java (enhanced)
│   ├── AuditService.java (new)
│   ├── CustomUserDetailsService.java (new)
│   └── DataInitializer.java (new)
└── config/
    ├── SecurityConfig.java (new)
    ├── WebSocketConfig.java (new)
    └── SchedulerConfig.java
```

## Migration Steps

### Step 1: Backup Existing Configuration
If you have existing scheduled tasks, document them:
- Task IDs
- Script paths
- Cron expressions

### Step 2: Update Dependencies
Replace your `pom.xml` with the enhanced version, which includes:
- Spring Security
- Spring Data JPA
- H2 Database
- WebSocket support

### Step 3: Database Initialization
On first run, the application will:
1. Create the H2 database
2. Create an admin user
3. Initialize empty task tables

### Step 4: Re-create Tasks
Since the data model has changed, you'll need to:
1. Login with admin credentials
2. Re-create your tasks through the UI
3. Set up any task dependencies

### Step 5: Update Scripts (Optional)
Your existing scripts will work as-is, but consider:
- Adding more descriptive output for better live monitoring
- Using exit codes appropriately (0 for success)
- Separating error messages to stderr

## Configuration Changes

### application.properties
The enhanced version adds:
```properties
# Database Configuration
spring.datasource.url=jdbc:h2:file:./data/scheduler
spring.jpa.hibernate.ddl-auto=update

# Security Session
server.servlet.session.timeout=30m
```

### Environment Variables
You can now pass environment variables to scripts:
- Configure in task details page
- Useful for parameterizing scripts

## API Changes

### Endpoints
- `/login` - New login page
- `/logout` - Logout endpoint
- `/audit` - View audit logs
- `/task/{id}` - Detailed task view
- `/api/tasks/{id}/running-executions` - REST API for live monitoring

### Security
All endpoints except `/login` and static resources require authentication.

## New Features to Leverage

### 1. Task Dependencies
- Chain tasks to create workflows
- Dependent tasks run automatically on parent success
- Configure in task details page

### 2. Live Output Monitoring
- Click 📡 to watch script output in real-time
- Useful for debugging long-running tasks
- Separates stdout and stderr

### 3. Audit Trail
- Every action is logged with user and timestamp
- IP address tracking for security
- Accessible via `/audit` endpoint

### 4. Dark Mode
- Toggle with 🌓 button
- Preference saved in browser

## Rollback Plan

If you need to rollback:
1. Stop the enhanced version
2. Delete the `./data` directory
3. Restore original code
4. Restart with original version

## Troubleshooting

### Can't Login
- Ensure you're using correct credentials
- Check application logs for errors
- Try incognito/private browsing mode

### Tasks Not Persisting
- Check database file permissions
- Ensure `./data` directory is writable
- Review application logs

### WebSocket Issues
- Check browser console for errors
- Ensure WebSocket traffic isn't blocked
- Try different browser

## Support

For issues or questions:
1. Check application logs first
2. Review this migration guide
3. Submit issues on GitHub

Remember to always test in a non-production environment first!
