# Detekt Helper Functions for AI Analysis
# 이 스크립트는 Detekt 결과를 AI가 쉽게 분석할 수 있도록 구조화된 형식으로 출력합니다

function Get-DetektReport {
    <#
    .SYNOPSIS
    Get Detekt analysis report in structured format

    .DESCRIPTION
    Parses Detekt XML report and returns structured data

    .EXAMPLE
    Get-DetektReport | Format-Table
    #>

    $xmlPath = "build\reports\detekt\detekt.xml"

    if (-not (Test-Path $xmlPath)) {
        Write-Warning "Detekt report not found. Run: .\gradlew.bat detekt"
        return
    }

    [xml]$report = Get-Content $xmlPath

    $issues = @()

    foreach ($file in $report.checkstyle.file) {
        foreach ($error in $file.error) {
            $issues += [PSCustomObject]@{
                File = $file.name -replace '.*\\src\\', 'src\'
                Line = $error.line
                Column = $error.column
                Severity = $error.severity
                Rule = $error.source -replace '.*\.', ''
                Message = $error.message
            }
        }
    }

    return $issues
}

function Get-DetektSummary {
    <#
    .SYNOPSIS
    Get summary of Detekt issues by category
    #>

    $issues = Get-DetektReport

    if (-not $issues) {
        Write-Host "No issues found or report not available." -ForegroundColor Green
        return
    }

    Write-Host "`n📊 Detekt Analysis Summary" -ForegroundColor Cyan
    Write-Host "=" * 50

    # Group by severity
    Write-Host "`n🔴 By Severity:" -ForegroundColor Yellow
    $issues | Group-Object Severity | Sort-Object Count -Descending | ForEach-Object {
        Write-Host "  $($_.Name): $($_.Count)" -ForegroundColor Gray
    }

    # Group by rule
    Write-Host "`n📋 Top Issues:" -ForegroundColor Yellow
    $issues | Group-Object Rule | Sort-Object Count -Descending | Select-Object -First 10 | ForEach-Object {
        Write-Host "  $($_.Name): $($_.Count)" -ForegroundColor Gray
    }

    # Most problematic files
    Write-Host "`n📁 Most Problematic Files:" -ForegroundColor Yellow
    $issues | Group-Object File | Sort-Object Count -Descending | Select-Object -First 5 | ForEach-Object {
        Write-Host "  $($_.Name): $($_.Count) issues" -ForegroundColor Gray
    }

    Write-Host "`n💡 Total Issues: $($issues.Count)" -ForegroundColor Cyan
    Write-Host "=" * 50
}

function Export-DetektReport {
    <#
    .SYNOPSIS
    Export Detekt report to JSON for easy parsing

    .PARAMETER OutputPath
    Path to save JSON report
    #>
    param(
        [string]$OutputPath = "build\reports\detekt\detekt-report.json"
    )

    $issues = Get-DetektReport

    if (-not $issues) {
        return
    }

    $report = @{
        timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        totalIssues = $issues.Count
        issues = $issues
        summary = @{
            bySeverity = $issues | Group-Object Severity | ForEach-Object { @{$_.Name = $_.Count} }
            byRule = $issues | Group-Object Rule | ForEach-Object { @{$_.Name = $_.Count} }
            byFile = $issues | Group-Object File | ForEach-Object { @{$_.Name = $_.Count} }
        }
    }

    $report | ConvertTo-Json -Depth 10 | Out-File -FilePath $OutputPath -Encoding UTF8

    Write-Host "📄 Report exported to: $OutputPath" -ForegroundColor Green
}

# Quick commands
function detekt-run { .\gradlew.bat detekt }
function detekt-show { Get-DetektSummary }
function detekt-report { Start-Process "build\reports\detekt\detekt.html" }
function detekt-export { Export-DetektReport }

# Export functions
Export-ModuleMember -Function Get-DetektReport, Get-DetektSummary, Export-DetektReport, detekt-run, detekt-show, detekt-report, detekt-export

Write-Host "Detekt helper functions loaded. Available commands:" -ForegroundColor Cyan
Write-Host "  - Get-DetektReport    : Get structured report data" -ForegroundColor Gray
Write-Host "  - Get-DetektSummary   : Show summary of issues" -ForegroundColor Gray
Write-Host "  - Export-DetektReport : Export to JSON" -ForegroundColor Gray
Write-Host "  - detekt-run          : Run Detekt analysis" -ForegroundColor Gray
Write-Host "  - detekt-show         : Show summary" -ForegroundColor Gray
Write-Host "  - detekt-report       : Open HTML report" -ForegroundColor Gray
Write-Host "  - detekt-export       : Export to JSON" -ForegroundColor Gray
