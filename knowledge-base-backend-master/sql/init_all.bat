@echo off
REM =====================================================
REM Enterprise Knowledge Base System - one-click microservice database initialization script (Windows)
REM =====================================================

setlocal enabledelayedexpansion

echo ========================================
echo Enterprise Knowledge Base System - Database Initialization
echo ========================================
echo.

set DB_HOST=localhost
set DB_PORT=3306
set DB_USER=root
set DB_PASS=123456

REM Check MySQL connection
echo [1/10] Checking MySQL connection...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -e "SELECT 1;" >nul 2>&1
if %errorlevel% equ 0 (
    echo √ MySQL connection successful
) else (
    echo × MySQL connection failed, please check the configuration
    pause
    exit /b 1
)

REM Create all databases
echo.
echo [2/10] Creating all databases...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% < sql\00_create_databases.sql
if %errorlevel% equ 0 (
    echo √ Databases created successfully
) else (
    echo × Database creation failed
    pause
    exit /b 1
)

REM Create kb_user tables
echo.
echo [3/10] Creating kb_user database tables...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% kb_user < sql\01_kb_user.sql
if %errorlevel% equ 0 (
    echo √ kb_user tables created successfully
) else (
    echo × kb_user table creation failed
)

REM Create kb_document tables
echo.
echo [4/10] Creating kb_document database tables...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% kb_document < sql\02_kb_document.sql
if %errorlevel% equ 0 (
    echo √ kb_document tables created successfully
) else (
    echo × kb_document table creation failed
)

REM Create kb_search tables
echo.
echo [5/10] Creating kb_search database tables...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% kb_search < sql\03_kb_search.sql
if %errorlevel% equ 0 (
    echo √ kb_search tables created successfully
) else (
    echo × kb_search table creation failed
)

REM Create kb_file tables
echo.
echo [6/10] Creating kb_file database tables...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% kb_file < sql\04_kb_file.sql
if %errorlevel% equ 0 (
    echo √ kb_file tables created successfully
) else (
    echo × kb_file table creation failed
)

REM Create kb_ai tables
echo.
echo [7/10] Creating kb_ai database tables...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% kb_ai < sql\05_kb_ai.sql
if %errorlevel% equ 0 (
    echo √ kb_ai tables created successfully
) else (
    echo × kb_ai table creation failed
)

REM Create kb_statistics tables
echo.
echo [8/10] Creating kb_statistics database tables...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% kb_statistics < sql\06_kb_statistics.sql
if %errorlevel% equ 0 (
    echo √ kb_statistics tables created successfully
) else (
    echo × kb_statistics table creation failed
)

REM Create kb_notification tables
echo.
echo [9/10] Creating kb_notification database tables...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% kb_notification < sql\07_kb_notification.sql
if %errorlevel% equ 0 (
    echo √ kb_notification tables created successfully
) else (
    echo × kb_notification table creation failed
)

REM Create kb_graph and kb_common tables
echo.
echo [10/10] Creating kb_graph and kb_common database tables...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% kb_graph < sql\08_kb_graph.sql
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% kb_common < sql\09_kb_common.sql
if %errorlevel% equ 0 (
    echo √ kb_graph and kb_common tables created successfully
) else (
    echo × kb_graph and kb_common table creation failed
)

REM Display summary information
echo.
echo ========================================
echo Database Initialization Summary
echo ========================================
echo.

mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% -e "SHOW DATABASES LIKE 'kb_%%';"

echo.
echo √ All databases and tables created successfully!
echo.
echo Next steps:
echo 1. Run the seed data scripts
echo 2. Start each microservice
echo.
pause
