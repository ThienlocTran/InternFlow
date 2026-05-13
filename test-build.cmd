@echo off
REM Script to test build before deploying to Render

echo ========================================
echo Testing Maven Build for Render Deploy
echo ========================================
echo.

echo [1/3] Cleaning previous build...
call mvnw.cmd clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed!
    exit /b 1
)
echo Clean successful!
echo.

echo [2/3] Building project (skipping tests)...
call mvnw.cmd package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    exit /b 1
)
echo Build successful!
echo.

echo [3/3] Checking JAR file...
if exist "target\InternFlow-0.0.1-SNAPSHOT.jar" (
    echo JAR file created successfully!
    dir target\InternFlow-0.0.1-SNAPSHOT.jar
) else (
    echo ERROR: JAR file not found!
    exit /b 1
)
echo.

echo ========================================
echo Build test completed successfully!
echo You can now deploy to Render.com
echo ========================================
