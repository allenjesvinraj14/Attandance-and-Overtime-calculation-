@echo off
setlocal enabledelayedexpansion
set SCRIPT_DIR=%~dp0
set WRAPPER_DIR=%SCRIPT_DIR%.mvn\wrapper

if not exist "%WRAPPER_DIR%\maven-wrapper.jar" (
  echo Downloading Maven wrapper JAR...
  mkdir "%WRAPPER_DIR%" 2>nul
  powershell -Command "Invoke-WebRequest -Uri https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar -OutFile \"%WRAPPER_DIR%\\maven-wrapper.jar\""
)

rem Run the wrapper JAR
java -jar "%WRAPPER_DIR%\maven-wrapper.jar" %*