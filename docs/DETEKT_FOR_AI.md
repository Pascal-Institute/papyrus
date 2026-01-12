# AI Assistant를 위한 Detekt 통합 가이드

## ✅ 설정 완료 항목

### 1. Detekt 플러그인 통합

-   ✅ `build.gradle.kts`에 Detekt 1.23.4 추가
-   ✅ 설정 파일: `config/detekt/detekt.yml`
-   ✅ Baseline 파일: Optional (없어도 작동)
-   ✅ 빌드 실패 방지: `ignoreFailures = true`
-   ✅ 다중 리포트: HTML, XML, TXT, SARIF

### 2. Helper Scripts

-   ✅ `check-code-quality.ps1`: 간편한 분석 및 리포트 확인
-   ✅ `detekt-helpers.ps1`: 구조화된 데이터 추출 함수들
-   ✅ Pre-commit hooks: 커밋 전 자동 검사

### 3. 현재 프로젝트 상태

-   📊 **총 이슈: 263개**
-   📁 리포트 위치: `build/reports/detekt/`
    -   HTML: 브라우저에서 확인 가능
    -   XML: 파싱 가능한 구조화된 데이터
    -   TXT: 간단한 텍스트 요약
    -   SARIF: GitHub 통합용

## 🤖 AI Assistant 사용 방법

### 코드 분석 요청 시

```powershell
# 1. Detekt 실행
.\gradlew.bat detekt

# 2. 요약 보기
. .\detekt-helpers.ps1
Get-DetektSummary

# 3. 상세 데이터 추출
Get-DetektReport | Where-Object { $_.Severity -eq "error" } | Format-Table

# 4. JSON으로 내보내기 (AI가 파싱하기 쉬움)
Export-DetektReport
```

### 특정 파일 분석

```powershell
# 특정 파일의 이슈만 필터링
Get-DetektReport | Where-Object { $_.File -like "*Form4Parser*" }

# 특정 규칙 위반만 보기
Get-DetektReport | Where-Object { $_.Rule -eq "UnusedPrivateMember" }
```

### 우선순위 이슈 식별

```powershell
# 가장 많은 이슈가 있는 파일 찾기
Get-DetektReport | Group-Object File | Sort-Object Count -Descending | Select-Object -First 10

# 가장 흔한 이슈 타입
Get-DetektReport | Group-Object Rule | Sort-Object Count -Descending
```

## 📊 현재 주요 이슈 카테고리

Based on the recent analysis (263 issues found):

### High Priority (수정 권장)

-   **UnusedPrivateMember**: 사용되지 않는 private 함수/변수
-   **UnusedParameter**: 사용되지 않는 함수 파라미터
-   **WildcardImport**: `import package.*` 형태의 임포트

### Medium Priority (리팩토링 고려)

-   **LongMethod**: 80줄 이상의 긴 메서드 (현재 threshold)
-   **ComplexMethod**: 복잡도가 높은 메서드

### Low Priority (스타일)

-   **MagicNumber**: 하드코딩된 숫자 (금융 계산에는 허용)
-   **MaxLineLength**: 150자 이상의 긴 줄

## 🔧 AI가 코드 개선 제안 시

### 1. Detekt 결과 참조

```kotlin
// ❌ Detekt가 지적할 코드
private fun unusedFunction() { ... }  // UnusedPrivateMember

// ✅ 개선안
// 함수 삭제 또는 실제로 사용
```

### 2. 컨텍스트 확인

```powershell
# 파일의 모든 이슈 확인
Get-DetektReport | Where-Object { $_.File -eq "src\main\kotlin\papyrus\Main.kt" }
```

### 3. 개선 후 재검증

```powershell
# 수정 후 다시 체크
.\gradlew.bat detekt
Get-DetektSummary  # 이슈 개수 감소 확인
```

## 🎯 Detekt 활용 워크플로우

### AI Assistant 코드 분석 순서:

1. **Before**: `.\gradlew.bat detekt` 실행
2. **Analyze**: `Get-DetektReport` 로 이슈 확인
3. **Focus**: 가장 많은 이슈가 있는 파일 우선 처리
4. **Fix**: 코드 개선 제안
5. **Verify**: 다시 detekt 실행하여 개선 확인
6. **Report**: 이슈 개수 감소 보고

### 예시 대화:

```
User: "Form4Parser 코드를 개선해줘"

AI:
1. Detekt 분석 중... ✓
2. Form4Parser.kt에서 5개 이슈 발견:
   - UnusedParameter: 'doc' 파라미터 미사용 (line 247)
   - LongMethod: extractNonDerivativeTransactions 메서드 너무 김 (180줄)
3. 개선 제안:
   - 'doc' 파라미터 제거 또는 사용
   - 메서드를 작은 함수들로 분할
4. 코드 수정...
5. 재검증: 이슈 5개 → 1개로 감소 ✓
```

## 📝 리포트 파일 구조

### XML Report (`detekt.xml`)

```xml
<checkstyle>
  <file name="path/to/File.kt">
    <error line="123" column="5"
           severity="warning"
           message="Issue description"
           source="detekt.RuleName" />
  </file>
</checkstyle>
```

### JSON Export (`detekt-report.json`)

```json
{
  "timestamp": "2026-01-12 10:30:00",
  "totalIssues": 263,
  "issues": [...],
  "summary": {
    "bySeverity": {...},
    "byRule": {...},
    "byFile": {...}
  }
}
```

## 💡 유용한 PowerShell 스니펫

### 이슈 트렌드 추적

```powershell
# 매일 실행하여 이슈 개수 기록
$count = (Get-DetektReport).Count
Add-Content "detekt-history.txt" "$(Get-Date -Format 'yyyy-MM-dd'): $count issues"
```

### 특정 타입 이슈 모두 보기

```powershell
Get-DetektReport |
  Where-Object { $_.Rule -eq "UnusedPrivateMember" } |
  Select-Object File, Line, Message |
  Format-Table -AutoSize
```

### 가장 문제가 많은 파일 리포트

```powershell
Get-DetektReport |
  Group-Object File |
  Sort-Object Count -Descending |
  Select-Object -First 5 |
  ForEach-Object {
    Write-Host "`n$($_.Name) - $($_.Count) issues:" -ForegroundColor Yellow
    $_.Group | Format-Table Line, Rule, Message -AutoSize
  }
```

## 🚀 다음 단계

1. **Baseline 생성** (선택사항):

    ```bash
    .\gradlew.bat detekt --create-baseline
    ```

    기존 이슈는 baseline에 기록, 새 코드만 엄격하게 체크

2. **CI/CD 통합**: GitHub Actions에서 자동 실행

3. **점진적 개선**: 매주 10개씩 이슈 해결 목표

4. **팀 규칙**: PR 전에 새로운 이슈 추가 금지

## 📚 참고 자료

-   Detekt Rules: https://detekt.dev/docs/rules/
-   프로젝트 문서: `docs/COMPILATION_ERROR_PREVENTION.md`
-   Pre-commit hooks: `pre-commit-hook.ps1`, `pre-commit-hook.sh`
