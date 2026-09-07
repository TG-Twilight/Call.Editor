param(
    [switch]$Online,
    [string]$KeystorePath = $env:REVIA_KEYSTORE,
    [string]$KeyAlias = $env:REVIA_KEY_ALIAS
)
$ErrorActionPreference = 'Stop'
$localSigning = Join-Path $PSScriptRoot 'signing.local.properties'
if (Test-Path -LiteralPath $localSigning) {
    $signing = Get-Content -Raw -Encoding UTF8 -LiteralPath $localSigning | ConvertFrom-StringData
    if (-not $KeystorePath) { $KeystorePath = $signing.KeystorePath }
    if (-not $KeyAlias) { $KeyAlias = $signing.KeyAlias }
}
if (-not $KeystorePath -or -not (Test-Path -LiteralPath $KeystorePath) -or -not $KeyAlias) {
    throw 'Set REVIA_KEYSTORE / REVIA_KEY_ALIAS or signing.local.properties before signing.'
}
$studioJbr = Join-Path $env:ProgramFiles 'Android/Android Studio/jbr'
if (Test-Path "$studioJbr/bin/javac.exe") { $env:JAVA_HOME = $studioJbr }
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME/bin/javac.exe")) { throw 'A full JDK is required.' }
$sdkRoot = $env:ANDROID_HOME
if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_SDK_ROOT }
if (-not $sdkRoot) { $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$sdkTools = Join-Path $sdkRoot 'build-tools/37.0.0'
if (-not (Test-Path "$sdkTools/zipalign.exe")) { throw 'Android Build Tools 37.0.0 is required.' }
$outputDir = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../dist'))
New-Item -ItemType Directory -Force $outputDir | Out-Null
$gradleArgs = @('-p', $PSScriptRoot, ':app:testReleaseUnitTest', ':app:assembleRelease', ':app:lintRelease')
if (-not $Online) { $gradleArgs += '--offline' }
& (Join-Path $PSScriptRoot 'gradlew.bat') @gradleArgs
if ($LASTEXITCODE -ne 0) { throw 'Gradle validation failed' }
$buildConfig = Get-Content -Raw -Encoding UTF8 (Join-Path $PSScriptRoot 'app/build.gradle.kts')
$versionName = [regex]::Match($buildConfig, 'versionName\s*=\s*"([A-Za-z0-9._-]+)"').Groups[1].Value
$versionCode = [regex]::Match($buildConfig, 'versionCode\s*=\s*(\d+)').Groups[1].Value
if (-not $versionName -or -not $versionCode) { throw 'Cannot read application version.' }
$unsigned = Join-Path $PSScriptRoot 'app/build/outputs/apk/release/app-release-unsigned.apk'
$aligned = Join-Path $PSScriptRoot 'app/build/outputs/apk/release/app-release-aligned.apk'
$signed = Join-Path $outputDir "CallEditor-$versionName-$versionCode.apk"
& "$sdkTools/zipalign.exe" -f -P 16 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw 'Alignment failed' }
$env:REVIA_KS_PASS = [Environment]::GetEnvironmentVariable('REVIA_KS_PASS', 'User')
if ([string]::IsNullOrEmpty($env:REVIA_KS_PASS)) { throw 'REVIA_KS_PASS is missing from the Windows user environment.' }
try {
    & "$sdkTools/apksigner.bat" sign --ks $KeystorePath --ks-key-alias $KeyAlias --ks-pass env:REVIA_KS_PASS --key-pass env:REVIA_KS_PASS --v4-signing-enabled false --out $signed $aligned
    if ($LASTEXITCODE -ne 0) { throw 'Signing failed' }
} finally { Remove-Item Env:REVIA_KS_PASS }
& "$sdkTools/zipalign.exe" -c -P 16 4 $signed
if ($LASTEXITCODE -ne 0) { throw 'Alignment verification failed' }
$verification = & "$sdkTools/apksigner.bat" verify --verbose --print-certs $signed
if ($LASTEXITCODE -ne 0) { throw 'Signature verification failed' }
if (($verification -join "`n") -notmatch 'ea385afc82e19824eea8a8868ed365df03e9886bbba33e47e1e04b43ec65cb67') { throw 'Unexpected certificate' }
$verification | Set-Content -Encoding UTF8 (Join-Path $outputDir 'signature-verification.txt')
Get-FileHash -Algorithm SHA256 $signed | Format-List | Out-String | Set-Content -Encoding UTF8 (Join-Path $outputDir 'SHA256.txt')
Write-Output $signed
