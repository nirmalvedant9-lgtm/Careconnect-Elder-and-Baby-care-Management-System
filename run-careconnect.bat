@echo off
echo ========================================================
echo        Starting CareConnect Full-Stack Application
echo ========================================================
echo.

echo [1/3] Checking and freeing port 8080 if in use...
powershell -NoProfile -Command "$conn = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue; if ($conn) { foreach ($c in $conn) { Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue; Write-Host ('Freed port 8080 from process ' + $c.OwningProcess) } } else { Write-Host 'Port 8080 is already free.' }"

echo.
echo [2/3] Starting Spring Boot Backend (Port 8080)...
start "CareConnect Backend (Spring Boot)" cmd /k "cd /d "%~dp0careconnect" && mvnw.cmd spring-boot:run"

echo.
echo [3/3] Starting React + Vite Frontend (Port 5173)...
start "CareConnect Frontend (Vite)" cmd /k "cd /d "%~dp0careconnect\careconnectfrontend" && npm run dev"

echo.
echo Waiting 5 seconds before opening browser...
timeout /t 5 >nul
start http://localhost:5173

echo ========================================================
echo   CareConnect is launching!
echo   Frontend: http://localhost:5173
echo   Backend:  http://localhost:8080/careconnect
echo ========================================================
pause
