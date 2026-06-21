@echo off
@REM Maven Wrapper for Windows - SmartScrabble
@REM Downloads Apache Maven 3.9.6 on first run if not cached.
@REM Usage:  .\mvnw.cmd clean javafx:run
setlocal

set "WRAPPER_DIR=%~dp0.mvn\wrapper"
set "MAVEN_VERSION=3.9.6"
set "MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"
set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

if not exist "%MVN_CMD%" (
    echo [mvnw] Apache Maven %MAVEN_VERSION% not found - downloading ...
    powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%WRAPPER_DIR%\maven.zip'"
    if errorlevel 1 (
        echo [mvnw] Download failed. Install Maven manually and add it to PATH.
        exit /B 1
    )
    powershell -NoProfile -Command "Expand-Archive -Path '%WRAPPER_DIR%\maven.zip' -DestinationPath '%WRAPPER_DIR%' -Force"
    del "%WRAPPER_DIR%\maven.zip"
    echo [mvnw] Maven %MAVEN_VERSION% ready.
)

call "%MVN_CMD%" %*
