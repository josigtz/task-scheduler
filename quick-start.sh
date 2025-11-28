#!/bin/bash

# Quick Start Script for Enhanced Cron Shell Scheduler
# This script helps you get started quickly

echo "🕒 Enhanced Cron Shell Scheduler - Quick Start"
echo "=============================================="

# Check Java version
echo "Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f 2 | cut -d'.' -f 1)
if [ "$java_version" -lt 17 ]; then
    echo "❌ Java 17 or higher is required. Current version: $java_version"
    exit 1
fi
echo "✅ Java version check passed"

# Check Maven
echo "Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "⚠️  Maven not found. Using Maven wrapper instead..."
    MVN="./mvnw"
    if [ ! -f "$MVN" ]; then
        echo "❌ Maven wrapper not found. Please install Maven."
        exit 1
    fi
else
    MVN="mvn"
fi
echo "✅ Maven check passed"

# Clean and build
echo ""
echo "Building the application..."
$MVN clean install
if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi
echo "✅ Build successful"

# Create sample scripts directory
echo ""
echo "Creating sample scripts..."
mkdir -p sample-scripts

# Create a simple test script
cat > sample-scripts/hello-world.sh << 'EOF'
#!/bin/bash
echo "Hello from Cron Shell Scheduler!"
echo "Current time: $(date)"
echo "This script runs successfully."
exit 0
EOF

chmod +x sample-scripts/hello-world.sh
echo "✅ Created sample script: $(pwd)/sample-scripts/hello-world.sh"

# Create a script with error handling
cat > sample-scripts/error-demo.sh << 'EOF'
#!/bin/bash
echo "Starting error demonstration script..."
echo "This will show how errors are handled."
echo "ERROR: This is an intentional error message!" >&2
exit 1
EOF

chmod +x sample-scripts/error-demo.sh
echo "✅ Created sample script: $(pwd)/sample-scripts/error-demo.sh"

# Instructions
echo ""
echo "🚀 Setup Complete!"
echo "=================="
echo ""
echo "To start the application, run:"
echo "  $MVN spring-boot:run"
echo ""
echo "Then open your browser to:"
echo "  http://localhost:8080"
echo ""
echo "Default credentials:"
echo "  Username: admin"
echo "  Password: admin123"
echo ""
echo "⚠️  IMPORTANT: Change the default password after first login!"
echo ""
echo "Sample scripts created in: $(pwd)/sample-scripts/"
echo "You can schedule these scripts to test the application."
echo ""
echo "Example cron expressions to try:"
echo "  */10 * * * * *  - Every 10 seconds"
echo "  0 * * * * *     - Every minute"
echo "  0 0 * * * *     - Every hour"
echo ""
echo "Happy scheduling! 🎉"
