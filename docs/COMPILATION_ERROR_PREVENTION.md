# 컴파일 에러 예방 가이드

프로젝트 규모가 커지면서 컴파일 에러가 자주 발생하는 것을 방지하기 위한 실용적인 가이드입니다.

## 🎯 핵심 원칙

### 1. **타입 안정성 최우선**

#### ❌ 나쁜 예:

```kotlin
fun processData(data: String?): String {
    return data.uppercase()  // NPE 위험!
}
```

#### ✅ 좋은 예:

```kotlin
fun processData(data: String?): String {
    return data?.uppercase() ?: "DEFAULT"  // Safe call + Elvis operator
}

// 또는 non-null로 선언
fun processData(data: String): String {
    require(data.isNotBlank()) { "Data cannot be blank" }
    return data.uppercase()
}
```

### 2. **명시적 타입 선언**

#### ❌ 컴파일러에 과도하게 의존:

```kotlin
val result = someComplexFunction()  // 타입이 불명확
```

#### ✅ 명시적 타입:

```kotlin
val result: FinancialResult = someComplexFunction()
```

### 3. **Early Return으로 null 체크**

```kotlin
fun analyzeDocument(content: String?): FinancialAnalysis {
    // Early return으로 null 조기 처리
    if (content.isNullOrBlank()) {
        return FinancialAnalysis.empty()
    }

    // 이후 코드에서는 content가 non-null임이 보장됨
    val cleanContent = content.trim()
    // ...
}
```

## 🔧 실용적인 도구 사용

### 1. IDE 설정 최적화 (IntelliJ IDEA)

**Settings > Editor > Inspections**:

-   ✅ Enable "Kotlin > Probable bugs" (모두 활성화)
-   ✅ Enable "Kotlin > Redundant constructs"
-   ✅ Enable "Kotlin > Style issues"

**Settings > Editor > Code Style > Kotlin**:

-   ✅ "Optimize imports on the fly"
-   ✅ "Add unambiguous imports on the fly"

**Settings > Build, Execution, Deployment > Compiler**:

-   ✅ "Build project automatically"
-   ⚠️ "Compile independent modules in parallel" (빠른 피드백)

### 2. Gradle Continuous Build

개발 중 자동으로 컴파일 체크:

```bash
.\gradlew.bat compileKotlin --continuous
```

변경사항이 있을 때마다 자동으로 컴파일을 시도합니다.

### 3. Detekt 활용

```bash
# 코드 품질 체크
.\gradlew.bat detekt

# HTML 리포트 확인
start build\reports\detekt\detekt.html
```

## 📋 체크리스트: 코드 작성 전

새로운 기능을 추가하기 전:

-   [ ] 관련 데이터 클래스가 `data class`로 정의되어 있는가?
-   [ ] Nullable 타입이 정말 필요한가? (non-null로 할 수 있는가?)
-   [ ] 외부 입력은 `require()` / `check()` / `?.` 로 검증했는가?
-   [ ] 복잡한 로직은 작은 함수로 분리했는가? (< 30줄)
-   [ ] 테스트 코드를 작성했는가?

## 🚀 작성 후 체크리스트

-   [ ] `.\gradlew.bat compileKotlin` 성공
-   [ ] `.\gradlew.bat test` 성공
-   [ ] IDE에 빨간 밑줄이 없음
-   [ ] 경고 메시지를 무시하지 않고 수정
-   [ ] Unused import 제거 (`Ctrl+Alt+O`)

## 💡 자주 발생하는 에러와 해결법

### 1. "Unresolved reference"

**원인**: Import 누락 또는 모듈 의존성 문제

**해결**:

```bash
# 1. Gradle 동기화
.\gradlew.bat build --refresh-dependencies

# 2. IntelliJ에서 Invalidate Caches
File > Invalidate Caches > Invalidate and Restart
```

### 2. "Type mismatch"

**원인**: Nullable과 Non-null 타입 혼용

**해결**:

-   Safe call 사용: `value?.property`
-   Elvis operator: `value ?: defaultValue`
-   Non-null assertion: `value!!` (최후의 수단)
-   `let` 블록 활용:
    ```kotlin
    value?.let { safeValue ->
        // safeValue는 non-null 보장
        doSomething(safeValue)
    }
    ```

### 3. "Smart cast to X is impossible"

**원인**: Nullable 체크 후 값이 변경될 수 있음

**해결**:

```kotlin
// ❌ var는 smart cast 불가
var value: String? = getValue()
if (value != null) {
    use(value)  // 에러: smart cast 불가
}

// ✅ 로컬 val로 복사
val value: String? = getValue()
if (value != null) {
    use(value)  // OK: smart cast 가능
}

// ✅ 또는 safe call + let
getValue()?.let { safeValue ->
    use(safeValue)  // OK
}
```

## 🎓 팀 규칙 제안

1. **커밋 전 필수 체크**: `.\gradlew.bat compileKotlin`
2. **PR 전 필수**: `.\gradlew.bat build` (전체 빌드 + 테스트)
3. **경고 무시 금지**: 모든 warning을 수정하거나 명시적으로 suppress
4. **주간 Detekt 리뷰**: 매주 Detekt 리포트 확인 및 개선
5. **코드 리뷰 시 타입 체크**: Nullable 타입 사용이 적절한지 검토

## 📊 진행 상황 모니터링

### 프로젝트 건강도 체크

```bash
# 1. 컴파일 가능 여부
.\gradlew.bat compileKotlin --console=plain

# 2. 테스트 통과율
.\gradlew.bat test --console=plain

# 3. 코드 품질 점수
.\gradlew.bat detekt --console=plain

# 4. 빌드 시간 측정
Measure-Command { .\gradlew.bat clean build }
```

### 목표 설정

-   🎯 **컴파일 성공률**: 100% (커밋 전 항상 체크)
-   🎯 **Detekt issues**: < 50개 유지
-   🎯 **테스트 커버리지**: > 60% (핵심 로직)
-   🎯 **빌드 시간**: < 2분 (증분 빌드)

## 🔄 CI/CD 파이프라인 (추후 구현)

GitHub Actions 예시:

```yaml
name: CI
on: [push, pull_request]
jobs:
    build:
        runs-on: ubuntu-latest
        steps:
            - uses: actions/checkout@v3
            - uses: actions/setup-java@v3
              with:
                  java-version: "17"
            - name: Compile
              run: ./gradlew compileKotlin
            - name: Run tests
              run: ./gradlew test
            - name: Detekt
              run: ./gradlew detekt
```

## 📚 참고 자료

-   [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
-   [Detekt Rules](https://detekt.dev/docs/rules/complexity)
-   [Effective Kotlin](https://kt.academy/book/effectivekotlin)
-   AGENTS.md (프로젝트 내부 원칙)
