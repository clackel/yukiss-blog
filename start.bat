@echo off
setlocal

set "PROJECT_ROOT=%~dp0"
set "BACKEND_DIR=%PROJECT_ROOT%blog-backend"
set "FRONTEND_DIR=%PROJECT_ROOT%blog-frontend"
if not defined BLOG_DB_PASSWORD (
    echo [WARN] BLOG_DB_PASSWORD is not set; the backend will use its configured default.
)

title Yukiss Blog Launcher

if not exist "%BACKEND_DIR%\pom.xml" (
    echo [ERROR] Backend project not found: "%BACKEND_DIR%"
    pause
    exit /b 1
)

if not exist "%FRONTEND_DIR%\package.json" (
    echo [ERROR] Frontend project not found: "%FRONTEND_DIR%"
    pause
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java was not found. Please install JDK 21 and add it to PATH.
    pause
    exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven was not found. Please install Maven and add it to PATH.
    pause
    exit /b 1
)

where npm >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm was not found. Please install Node.js and add it to PATH.
    pause
    exit /b 1
)

if not exist "%FRONTEND_DIR%\node_modules" (
    echo [INFO] Installing frontend dependencies for the first run...
    pushd "%FRONTEND_DIR%"
    call npm install
    if errorlevel 1 (
        popd
        echo [ERROR] Failed to install frontend dependencies.
        pause
        exit /b 1
    )
    popd
)

echo [INFO] Starting backend at http://localhost:4000 ...
start "Yukiss Blog - Backend" cmd /k "cd /d ""%BACKEND_DIR%"" && mvn clean spring-boot:run"

echo [INFO] Starting frontend at http://127.0.0.1:3000 ...
start "Yukiss Blog - Frontend" cmd /k "cd /d ""%FRONTEND_DIR%"" && npm run dev"

echo [OK] Frontend and backend were launched in separate windows.
echo      Close those two windows to stop the services.
timeout /t 3 /nobreak >nul

endlocal
