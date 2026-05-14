@echo off
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
    echo Java was not found on PATH.
    echo Install Java 21 or run this from IntelliJ instead.
    pause
    exit /b 1
)

java --module-path "lib;ucd-gpa-calculator-1.0.0.jar" --module ie.ucd.gpa/ie.ucd.gpa.UcdGpaCalculatorApp

if errorlevel 1 (
    echo.
    echo The GPA calculator could not start.
    pause
)
