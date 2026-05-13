#!/bin/bash
# Script to test build before deploying to Render

echo "========================================"
echo "Testing Maven Build for Render Deploy"
echo "========================================"
echo ""

echo "[1/3] Cleaning previous build..."
./mvnw clean
if [ $? -ne 0 ]; then
    echo "ERROR: Clean failed!"
    exit 1
fi
echo "Clean successful!"
echo ""

echo "[2/3] Building project (skipping tests)..."
./mvnw package -DskipTests
if [ $? -ne 0 ]; then
    echo "ERROR: Build failed!"
    exit 1
fi
echo "Build successful!"
echo ""

echo "[3/3] Checking JAR file..."
if [ -f "target/InternFlow-0.0.1-SNAPSHOT.jar" ]; then
    echo "JAR file created successfully!"
    ls -lh target/InternFlow-0.0.1-SNAPSHOT.jar
else
    echo "ERROR: JAR file not found!"
    exit 1
fi
echo ""

echo "========================================"
echo "Build test completed successfully!"
echo "You can now deploy to Render.com"
echo "========================================"
