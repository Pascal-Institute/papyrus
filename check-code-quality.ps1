# Detekt 분석 및 리포트 확인 스크립트
# 사용법: .\check-code-quality.ps1

Write-Host "🔍 Running Detekt code analysis..." -ForegroundColor Cyan

# Detekt 실행
.\gradlew.bat detekt --console=plain

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ Detekt analysis completed!" -ForegroundColor Green

    # 리포트 파일 경로
    $htmlReport = "build\reports\detekt\detekt.html"
    $txtReport = "build\reports\detekt\detekt.txt"
    $xmlReport = "build\reports\detekt\detekt.xml"

    # 텍스트 리포트가 있으면 요약 출력
    if (Test-Path $txtReport) {
        Write-Host "`n📊 Issues Summary:" -ForegroundColor Yellow
        Get-Content $txtReport | Select-Object -First 50
    }

    # HTML 리포트 열기
    if (Test-Path $htmlReport) {
        Write-Host "`n🌐 Opening HTML report in browser..." -ForegroundColor Cyan
        Start-Process $htmlReport
    }

    Write-Host "`n📁 Report files:" -ForegroundColor Cyan
    Write-Host "  - HTML: $htmlReport" -ForegroundColor Gray
    Write-Host "  - Text: $txtReport" -ForegroundColor Gray
    Write-Host "  - XML:  $xmlReport" -ForegroundColor Gray

} else {
    Write-Host "`n⚠️  Detekt found issues, but build didn't fail (ignoreFailures=true)" -ForegroundColor Yellow
    Write-Host "Check the reports for details." -ForegroundColor Yellow
}
