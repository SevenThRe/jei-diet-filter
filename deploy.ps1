# deploy.ps1 — 重新构建并把 jar 部署到整合包 mods/
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
& .\gradlew.bat build --console=plain
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Copy-Item "build\libs\jei_diet_filter-1.0.0.jar" "..\..\" -Force
Write-Host "deployed to mods/"
