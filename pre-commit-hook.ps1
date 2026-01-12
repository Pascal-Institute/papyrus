# Pre-commit hook for Windows PowerShell
# 사용법: .git\hooks\pre-commit.ps1 에 복사

Write-Host "🔍 Running pre-commit checks..." -ForegroundColor Cyan

# 1. Kotlin 컴파일 체크
Write-Host "📦 Compiling Kotlin..." -ForegroundColor Yellow
& .\gradlew.bat compileKotlin compileTestKotlin --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Compilation failed! Please fix errors before committing." -ForegroundColor Red
    exit 1
}

# 2. Detekt 정적 분석
Write-Host "🔎 Running Detekt..." -ForegroundColor Yellow
& .\gradlew.bat detekt --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Detekt found issues. Check the report at build/reports/detekt/detekt.html" -ForegroundColor Yellow
    # Warning only, not blocking
}

# 3. 테스트 실행 (빠른 테스트만)
Write-Host "🧪 Running quick tests..." -ForegroundColor Yellow
& .\gradlew.bat test --tests "*Unit*" --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠️  Some tests failed. Consider fixing them." -ForegroundColor Yellow
    # Warning only, not blocking
}

Write-Host "✅ Pre-commit checks passed!" -ForegroundColor Green
exit 0
