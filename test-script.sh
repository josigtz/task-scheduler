#!/bin/bash

# Sample test script for Cron Shell Scheduler
# This script demonstrates various output scenarios

echo "🚀 Test script started at $(date)"
echo "Running on host: $(hostname)"
echo "Current user: $(whoami)"

# Simulate some work
echo "Processing data..."
sleep 2

# Generate some output
echo "✅ Step 1: Data validation completed"
sleep 1

echo "📊 Step 2: Generating report..."
sleep 1

# Demonstrate error output (to stderr)
echo "⚠️ Warning: This is a warning message" >&2

# Final status
echo "✅ Test script completed successfully!"
echo "Total execution time: $SECONDS seconds"

# Exit successfully
exit 0
