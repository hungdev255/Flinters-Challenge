@echo off
REM One-shot runner for Windows cmd.exe — does not depend on PowerShell
REM execution policy. Sets JAVA_HOME, falls back through common JDK 21
REM install paths, then runs the pre-built aggregator jar.
REM
REM Usage:  run.cmd           (double-click OK — window stays open at end)
REM         run.cmd /nopause  (for scripts/CI — exit immediately)

setlocal

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto :have_jdk

for %%P in (
    "%USERPROFILE%\.jdks\corretto-21.0.10"
    "C:\Program Files\Eclipse Adoptium\jdk-21"
    "C:\Program Files\Amazon Corretto\jdk21"
) do (
    if exist "%%~P\bin\java.exe" (
        set "JAVA_HOME=%%~P"
        goto :have_jdk
    )
)

echo Could not find a JDK 21 install. Set JAVA_HOME manually and re-run.
exit /b 1

:have_jdk
echo Using JDK: %JAVA_HOME%
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "ad_data.csv" (
    if exist "ad_data.csv.zip" (
        echo Unzipping ad_data.csv.zip ...
        powershell -NoProfile -Command "Expand-Archive -Path .\ad_data.csv.zip -DestinationPath . -Force"
    ) else (
        echo ad_data.csv not found.
        exit /b 1
    )
)

if exist "target\aggregator.jar" (
    set "JAR=target\aggregator.jar"
) else if exist "dist\aggregator.jar" (
    set "JAR=dist\aggregator.jar"
) else (
    echo No jar found. Run "mvn package" first or restore dist\aggregator.jar.
    exit /b 1
)

java -XX:+UseG1GC -Xmx128m -jar "%JAR%" --input ad_data.csv --output results

if /i not "%~1"=="/nopause" (
    echo.
    pause
)
