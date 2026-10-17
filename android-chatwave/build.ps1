param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs = @("assembleDebug")
)

$ErrorActionPreference = "Stop"
$gradleVersion = "8.9"
$cacheRoot = Join-Path $HOME ".gradle\rea-bootstrap"
$gradleHome = Join-Path $cacheRoot "gradle-$gradleVersion"
$gradleExe = Join-Path $gradleHome "bin\gradle.bat"

$sdkRoot = $env:ANDROID_HOME
if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_SDK_ROOT }
if (-not $sdkRoot) { $sdkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk" }

if (-not (Test-Path (Join-Path $sdkRoot "platforms\android-35\android.jar"))) {
    throw "Android SDK Platform 35 not found. Install Android SDK Platform 35 and Build-Tools 35.0.0 in Android Studio > SDK Manager. If the SDK is elsewhere, set ANDROID_HOME first."
}

$localProperties = Join-Path $PSScriptRoot "local.properties"
if (-not (Test-Path $localProperties)) {
    $sdkForGradle = $sdkRoot.Replace("\", "/")
    Set-Content -Path $localProperties -Value "sdk.dir=$sdkForGradle" -Encoding ASCII
}

if (-not (Test-Path $gradleExe)) {
    New-Item -ItemType Directory -Force -Path $cacheRoot | Out-Null
    $archive = Join-Path $env:TEMP "gradle-$gradleVersion-bin.zip"
    $uri = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
    Write-Host "Downloading official Gradle $gradleVersion distribution..."
    Invoke-WebRequest -Uri $uri -OutFile $archive
    $actualHash = (Get-FileHash -Path $archive -Algorithm SHA256).Hash.ToLowerInvariant()
    $expectedHash = "d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab"
    if ($actualHash -ne $expectedHash) {
        Remove-Item -Force $archive -ErrorAction SilentlyContinue
        throw "Gradle archive SHA-256 verification failed. The archive was deleted."
    }
    Expand-Archive -Path $archive -DestinationPath $cacheRoot -Force
    Remove-Item -Force $archive
}

if (-not (Test-Path $gradleExe)) {
    throw "Gradle executable was not found after extraction: $gradleExe"
}

Push-Location $PSScriptRoot
try {
    & $gradleExe @GradleArgs
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
