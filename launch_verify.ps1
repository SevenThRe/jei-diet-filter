# launch_verify.ps1 — 启动真实整合包并自动进入"新的世界"，验证 jei_diet_filter 注入与日志
# 用法: powershell -File launch_verify.ps1 [-MaxMinutes 12]
param([int]$MaxMinutes = 12)
$ErrorActionPreference = 'Stop'

$ver      = "e:\Game Files (x86)\Minecraft\.minecraft\versions\COY3"
$mcRoot   = "e:\Game Files (x86)\Minecraft\.minecraft"
$java     = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot\bin\java.exe"
$logFile  = "$PSScriptRoot\launch.log"
$json     = Get-Content "$ver\COY3.json" -Raw -Encoding UTF8 | ConvertFrom-Json

# ---- classpath: 解析 libraries（共享根优先，跳过 natives classifier 与被 rules 排除的） ----
$roots = @("$mcRoot\libraries", "$ver\libraries")
$cp = New-Object System.Collections.Generic.List[string]
foreach ($lib in $json.libraries) {
    if ($lib.rules) {
        $allow = $false
        foreach ($r in $lib.rules) {
            if ($r.action -eq 'allow') { if (-not $r.os -or $r.os.name -eq 'windows') { $allow = $true } }
            elseif ($r.action -eq 'disallow' -and (-not $r.os -or $r.os.name -eq 'windows')) { $allow = $false }
        }
        if (-not $allow) { continue }
    }
    $p = $lib.downloads.artifact.path
    if (-not $p) { continue }
    foreach ($root in $roots) {
        $cand = Join-Path $root ($p -replace '/', '\')
        if (Test-Path $cand) { $cp.Add($cand); break }
    }
}
$cp.Add("$ver\COY3.jar")
$cpStr = $cp -join ';'
Write-Host "classpath entries: $($cp.Count)"

# ---- 资产目录 ----
$assetsRoot = if (Test-Path "$ver\assets") { "$ver\assets" } else { "$mcRoot\assets" }

# ---- JVM 参数模板替换 ----
$repl = @{
    '\$\{natives_directory\}'   = "$ver\COY3-natives"
    '\$\{library_directory\}'   = "$mcRoot\libraries"
    '\$\{classpath_separator\}' = ';'
    '\$\{version_name\}'        = 'COY3'
    '\$\{launcher_name\}'       = 'trae-verify'
    '\$\{launcher_version\}'    = '1.0'
    '\$\{classpath\}'           = $cpStr
}
$jvmArgs = @()
foreach ($a in $json.arguments.jvm) {
    if ($a -is [string]) { $s = $a } else { continue }  # 带 rules 的对象（如 quickPlay）跳过，手动追加
    foreach ($k in $repl.Keys) { $s = $s -replace $k, $repl[$k] }
    $jvmArgs += $s
}

# ---- 游戏参数模板替换 ----
$grepl = @{
    '\$\{auth_player_name\}'   = 'SevenThRe'
    '\$\{auth_uuid\}'          = '00000000-0000-3009-9b3a-a537071e8a0b'
    '\$\{auth_access_token\}'  = '0'
    '\$\{user_type\}'          = 'legacy'
    '\$\{version_type\}'       = 'COY3'
    '\$\{game_directory\}'     = $ver
    '\$\{assets_root\}'        = $assetsRoot
    '\$\{assets_index_name\}'  = $json.assetIndex.id
    '\$\{user_properties\}'    = '{}'
    '\$\{resolution_width\}'   = '854'
    '\$\{resolution_height\}'  = '480'
}
$gameArgs = @()
foreach ($a in $json.arguments.game) {
    if ($a -is [string]) { $s = $a } else { continue }
    foreach ($k in $grepl.Keys) { $s = $s -replace $k, $grepl[$k] }
    $gameArgs += $s
}
$gameArgs += @('--quickPlaySingleplayer', '新的世界')

$mem = @('-Xms4G', '-Xmx16G', '-XX:+UseG1GC', '-XX:+ParallelRefProcEnabled', '-XX:MaxGCPauseMillis=200',
         '-XX:+UnlockExperimentalVMOptions', '-XX:G1NewSizePercent=30', '-XX:G1MaxNewSizePercent=40',
         '-XX:G1HeapRegionSize=8M', '-XX:G1ReservePercent=20', '-XX:InitiatingHeapOccupancyPercent=15')

$allArgs = $mem + $jvmArgs + @($json.mainClass) + $gameArgs
$argFile = "$PSScriptRoot\launch_args.txt"
Set-Content -Path $argFile -Value ($allArgs -join "`n") -Encoding UTF8
Write-Host "args written to $argFile"

# Start-Process 用空格拼接数组，必须自行给含空格的参数加引号
$argLine = ($allArgs | ForEach-Object {
    if ($_ -match '[\s"]') { '"' + ($_ -replace '"', '\"') + '"' } else { $_ }
}) -join ' '

if (Test-Path $logFile) { Remove-Item $logFile -Force }
$proc = Start-Process -FilePath $java -ArgumentList $argLine -WorkingDirectory $ver `
        -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err" -PassThru
Write-Host "game pid: $($proc.Id)"

# ---- 轮询日志找成功/失败标记 ----
$deadline = (Get-Date).AddMinutes($MaxMinutes)
$found = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 15
    if ($proc.HasExited) { Write-Host "game exited early, code=$($proc.ExitCode)"; break }
    $tail = Get-Content $logFile -ErrorAction SilentlyContinue
    $err  = Get-Content "$logFile.err" -ErrorAction SilentlyContinue
    $all  = @($tail) + @($err)
    if ($all | Select-String -Pattern 'Registered uneaten-food prefix' -Quiet) { Write-Host "FOUND: prefix registered (ElementPrefixParser constructed)"; $found = $true; Start-Sleep -Seconds 10; break }
    if ($all | Select-String -Pattern 'Mixin apply failed|InjectionError|Unexpected error|has crashed' -Quiet) { Write-Host "FOUND: error markers"; break }
}

Write-Host "=== JEIDietFilter / mixin / diet lines ==="
$tail = Get-Content $logFile -ErrorAction SilentlyContinue; $err = Get-Content "$logFile.err" -ErrorAction SilentlyContinue
(@($tail) + @($err)) | Select-String -Pattern 'JEIDietFilter|Mixin apply|InjectionError|ElementPrefixParser|jei_diet_filter|diet' | Select-Object -First 40 | ForEach-Object { $_.Line }

if (-not $proc.HasExited) { Stop-Process -Id $proc.Id -Force; Write-Host "game stopped" }
Write-Host "VERIFY_MARKERS_FOUND=$found"
