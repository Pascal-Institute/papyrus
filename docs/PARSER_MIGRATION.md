# Parser Migration Summary

## 변경 일시
2026-01-09

## 변경 내용
`src\main\kotlin\papyrus\core\service\parser` 디렉토리를 완전히 제거하고, 모든 파싱 기능을 `ahmes` 라이브러리로 대체했습니다.

## 삭제된 파일들

### Main Source
- `src\main\kotlin\papyrus\core\service\parser\EnhancedFinancialParser.kt`
- `src\main\kotlin\papyrus\core\service\parser\FinancialDataMapper.kt`
- `src\main\kotlin\papyrus\core\service\parser\InlineXbrlExtractor.kt`
- `src\main\kotlin\papyrus\core\service\parser\SecTableParser.kt`
- `src\main\kotlin\papyrus\core\service\parser\SecTextNormalization.kt`
- `src\main\kotlin\papyrus\core\service\parser\XbrlCompanyFactsExtractor.kt`
- `src\main\kotlin\papyrus\core\service\parser\extension\ParserExtensions.kt`
- `src\main\kotlin\papyrus\util\file\PdfParser.kt`

### Test Source
- `src\test\kotlin\papyrus\core\service\parser\ParserFactoryTest.kt`
- `src\test\kotlin\papyrus\core\service\parser\InlineXbrlExtractorTest.kt`
- `src\test\kotlin\papyrus\core\service\parser\HtmlParserUnitTest.kt`

## 수정된 파일들

### 1. `src\main\kotlin\papyrus\ui\AnalyzeView.kt`
**변경 전:**
```kotlin
import papyrus.core.service.parser.XbrlCompanyFactsExtractor
```

**변경 후:**
```kotlin
import com.pascal.institute.ahmes.parser.XbrlCompanyFactsExtractor
```

### 2. `src\main\kotlin\papyrus\core\model\AIModels.kt`
**변경 전:**
```kotlin
import papyrus.util.file.SecSectionType
```

**변경 후:**
```kotlin
import com.pascal.institute.ahmes.util.SecSectionType
```

### 3. `src\main\kotlin\papyrus\core\service\analyzer\FinancialAnalyzer.kt`
이미 `ahmes` 라이브러리를 사용 중:
```kotlin
import com.pascal.institute.ahmes.parser.EnhancedFinancialParser
import com.pascal.institute.ahmes.parser.InlineXbrlExtractor
import com.pascal.institute.ahmes.parser.SecTableParser
```

## Ahmes 라이브러리 사용 현황

### 파서 (Parser)
- `com.pascal.institute.ahmes.parser.EnhancedFinancialParser`
- `com.pascal.institute.ahmes.parser.InlineXbrlExtractor`
- `com.pascal.institute.ahmes.parser.SecTableParser`
- `com.pascal.institute.ahmes.parser.XbrlCompanyFactsExtractor`

### 포맷 파서 (Format Parser)
- `com.pascal.institute.ahmes.format.ParserFactory`
- `com.pascal.institute.ahmes.format.HtmlParser`
- `com.pascal.institute.ahmes.format.PdfFormatParser`
- `com.pascal.institute.ahmes.format.TxtParser`

### 유틸리티 (Utility)
- `com.pascal.institute.ahmes.util.SecSectionType`
- `com.pascal.institute.ahmes.util.PdfParser`

### 모델 (Model)
`src\main\kotlin\papyrus\core\model\ParserModels.kt`에서 typealias로 정의:
- `ExtendedFinancialMetric`
- `MetricCategory`
- `PeriodType`
- `StructuredFinancialData`
- 등 모든 파싱 관련 모델

## 이점

1. **코드 중복 제거**: 로컬 파서 코드가 제거되어 중복이 사라짐
2. **유지보수 용이**: `ahmes` 라이브러리에서 통합 관리
3. **기능 향상**: `ahmes`의 최신 기능 (AI 분석, XBRL 추출 등) 활용 가능
4. **일관성**: 모든 SEC 파싱 로직이 하나의 라이브러리로 통합
5. **테스트 간소화**: 라이브러리 레벨에서 테스트 진행

## 검증 필요 사항

### 빌드 확인
Java 환경이 설정되어 있다면 다음 명령으로 빌드 확인:
```bash
.\gradlew.bat clean build
```

### 테스트 실행
```bash
.\gradlew.bat test
```

### 애플리케이션 실행
```bash
.\gradlew.bat run
```

## 주의사항

현재 환경에 Java가 설치되어 있지 않아 빌드 검증이 완료되지 않았습니다.
빌드를 실행하려면:

1. Java 17 이상 설치
2. JAVA_HOME 환경변수 설정
3. 위의 검증 명령 실행

또는 IntelliJ IDEA에서 프로젝트를 열어 빌드 및 테스트를 실행할 수 있습니다.

## 변경 영향도

- **높은 영향**: 파서 관련 기능 전체
- **중간 영향**: 재무 분석 로직 (이미 ahmes 사용 중이었음)
- **낮은 영향**: UI 레이어 (import 문만 변경)

## 롤백 방법

필요시 Git에서 이전 커밋으로 되돌릴 수 있습니다:
```bash
git log --oneline
git revert <commit-hash>
```
