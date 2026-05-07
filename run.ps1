# Convenience launcher: sets JAVA_HOME + PATH for this session, then runs the
# pre-built aggregator jar against ad_data.csv. Reviewer-friendly — no global
# install required if a JDK 21 already lives in one of the common locations.

$ErrorActionPreference = 'Stop'

# 1. Locate a Java 21 install. Prefer JAVA_HOME if already set; otherwise
#    probe the typical Windows JDK locations.
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $candidates = @(
        "$env:USERPROFILE\.jdks\corretto-21.0.10",
        "$env:USERPROFILE\.jdks\corretto-21*",
        "$env:USERPROFILE\.jdks\temurin-21*",
        "$env:USERPROFILE\.jdks\graalvm-jdk-2*",
        'C:\Program Files\Eclipse Adoptium\jdk-21*',
        'C:\Program Files\Amazon Corretto\jdk21*',
        'C:\Program Files\Java\jdk-21*'
    )
    foreach ($pat in $candidates) {
        $hit = Get-Item $pat -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($hit) { $env:JAVA_HOME = $hit.FullName; break }
    }
}

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Error "Could not find a JDK 21 install. Set `$env:JAVA_HOME manually and re-run."
}

$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Write-Host "Using JDK: $env:JAVA_HOME" -ForegroundColor Cyan

# 2. Ensure the dataset is unzipped.
if (-not (Test-Path .\ad_data.csv)) {
    if (Test-Path .\ad_data.csv.zip) {
        Write-Host "Unzipping ad_data.csv.zip ..." -ForegroundColor Cyan
        Expand-Archive .\ad_data.csv.zip -DestinationPath . -Force
    } else {
        Write-Error "ad_data.csv (and ad_data.csv.zip) not found in current directory."
    }
}

# 3. Pick the jar: prefer a freshly-built target/ jar, fall back to the
#    pre-built dist/ jar that is always committed (no Maven required).
if (Test-Path .\target\aggregator.jar) {
    $jar = '.\target\aggregator.jar'
} elseif (Test-Path .\dist\aggregator.jar) {
    $jar = '.\dist\aggregator.jar'
} else {
    Write-Error "No jar found. Run 'mvn package' first or restore dist\aggregator.jar."
}

# 4. Run.
Write-Host "Running aggregator ($jar) ..." -ForegroundColor Green
java -XX:+UseG1GC -Xmx128m -jar $jar --input ad_data.csv --output results
