# SEC Report Type-Specific Parsers

이 디렉토리는 SEC 보고서 타입별 전문 파서를 포함합니다.

## 📋 구조

### 파서 아키텍처

```
SecReportParser (인터페이스)
    ↓
BaseSecReportParser (추상 클래스 - 공통 기능)
    ↓
    ├── Form10KParser      - 10-K 연간 보고서
    ├── Form10QParser      - 10-Q 분기 보고서
    ├── Form8KParser       - 8-K 현재 보고서
    ├── FormS1Parser       - S-1 IPO 등록서
    ├── FormDEF14AParser   - DEF 14A 위임장
    ├── Form20FParser      - 20-F 외국기업 연간보고서
    └── GenericSecReportParser - 범용 파서
```

## 🚀 사용 방법

### 기본 사용법

```kotlin
import papyrus.core.service.parser.*
import papyrus.core.model.*

// 1. 보고서 타입으로 파서 가져오기
val parser = SecReportParserFactory.getParserByFormType("10-K")

// 2. 메타데이터 생성
val metadata = SecReportMetadata(
    formType = "10-K",
    companyName = "Apple Inc.",
    cik = "0000320193",
    filingDate = "2024-10-27",
    accessionNumber = "0000320193-24-000123"
)

// 3. HTML 콘텐츠 파싱
val htmlContent = loadSecReport() // SEC API에서 가져온 HTML
val result = parser.parseHtml(htmlContent, metadata)

// 4. 파싱 결과 사용
when (result) {
    is Form10KParseResult -> {
        println("Business Description: ${result.businessDescription}")
        println("Risk Factors: ${result.riskFactors.size}")
        println("MD&A: ${result.mdAndA?.executiveSummary}")
    }
}
```

### 자동 파싱

```kotlin
// 콘텐츠 타입을 자동으로 감지하고 파싱
val result = SecReportParsingUtils.parseReport(
    content = htmlContent,
    formType = "10-K",
    metadata = metadata
)
```

## 📊 파서별 특징

### 1. **Form10KParser** (10-K 연간 보고서)

가장 포괄적인 연간 재무 보고서 파서

**추출 항목:**
- ✅ Item 1: Business Description
- ✅ Item 1A: Risk Factors
- ✅ Item 2: Properties
- ✅ Item 3: Legal Proceedings
- ✅ Item 7: MD&A (Management Discussion & Analysis)
- ✅ Item 8: Financial Statements (감사됨)
- ✅ Item 9A: Controls and Procedures
- ✅ Item 10: Directors and Officers
- ✅ Item 11: Executive Compensation
- ✅ Item 15: Exhibits

**사용 예시:**
```kotlin
val parser = Form10KParser()
val result = parser.parseHtml(htmlContent, metadata)

// Business 섹션 추출
println(result.businessDescription)

// 리스크 요인 분석
result.riskFactors.forEach { risk ->
    println("${risk.category}: ${risk.description}")
}

// MD&A 요약
result.mdAndA?.let { mda ->
    println("Executive Summary: ${mda.executiveSummary}")
    println("Results of Operations: ${mda.resultsOfOperations}")
    println("Liquidity: ${mda.liquidityAndCapitalResources}")
}
```

### 2. **Form10QParser** (10-Q 분기 보고서)

분기별 재무 정보를 파싱

**추출 항목:**
- ✅ Part I, Item 1: Financial Statements (미감사)
- ✅ Part I, Item 2: MD&A
- ✅ Part I, Item 3: Market Risk Disclosures
- ✅ Part I, Item 4: Controls and Procedures
- ✅ Part II, Item 1A: Risk Factors (변경사항만)
- ✅ 분기 정보 자동 감지 (Q1, Q2, Q3)

**사용 예시:**
```kotlin
val parser = Form10QParser()
val result = parser.parseHtml(htmlContent, metadata)

// 분기 정보
println("Quarter: ${result.quarter}") // "Q1", "Q2", etc.
println("Fiscal Year: ${result.fiscalYear}")

// 분기별 MD&A
println(result.mdAndA?.resultsOfOperations)
```

### 3. **Form8KParser** (8-K 현재 보고서)

중요 이벤트 발생 시 제출되는 보고서 파서

**추출 항목:**
- ✅ 이벤트 날짜
- ✅ 보고된 Item 목록 (Item 2.02, Item 5.02 등)
- ✅ 재무 결과 (Item 2.02)
- ✅ M&A (Item 2.01)
- ✅ 경영진 변동 (Item 5.02)
- ✅ 파산 (Item 1.03)
- ✅ 중요도 점수 계산

**사용 예시:**
```kotlin
val parser = Form8KParser()
val result = parser.parseHtml(htmlContent, metadata)

// 이벤트 정보
println("Event Date: ${result.eventDate}")
println("Items Reported: ${result.eventItems}")

// 중요도 평가
val importance = parser.calculateImportanceScore(result)
println("Importance Score: $importance")

// 특정 이벤트 확인
if (result.executiveChanges != null) {
    println("Executive Change: ${result.executiveChanges}")
}

if (result.bankruptcy != null) {
    println("⚠️ CRITICAL: Bankruptcy event!")
}
```

### 4. **FormS1Parser** (S-1 IPO 등록서)

IPO 등록서 파서

**추출 항목:**
- ✅ Prospectus Summary
- ✅ Business Description
- ✅ Risk Factors
- ✅ Use of Proceeds
- ✅ Dilution
- ✅ Financial Statements (3-5년)
- ✅ Underwriting
- ✅ Offering Price (공모가)
- ✅ Shares Offered

**사용 예시:**
```kotlin
val parser = FormS1Parser()
val result = parser.parseHtml(htmlContent, metadata)

// IPO 정보
println("Offering Price: ${result.offeringPrice}")
println("Shares Offered: ${result.sharesOffered}")
println("Use of Proceeds: ${result.useOfProceeds}")
```

### 5. **FormDEF14AParser** (DEF 14A 위임장)

주주총회 위임장 파서

**추출 항목:**
- ✅ 주주총회 날짜
- ✅ 의결 사항 목록
- ✅ 경영진 보상
- ✅ 이사회 정보
- ✅ 기업 지배구조

**사용 예시:**
```kotlin
val parser = FormDEF14AParser()
val result = parser.parseHtml(htmlContent, metadata)

// 주주총회 정보
println("Meeting Date: ${result.meetingDate}")

// 의결 사항
result.votingMatters.forEach { matter ->
    println("Voting Matter: $matter")
}

// 경영진 보상
println(result.executiveCompensation)
```

### 6. **Form20FParser** (20-F 외국기업 연간보고서)

외국 기업용 연간 보고서 파서 (10-K와 유사)

**추출 항목:**
- ✅ Business Description (Item 4)
- ✅ Risk Factors
- ✅ Financial Statements (IFRS 또는 US GAAP)
- ✅ MD&A (Item 5)
- ✅ Country of Incorporation
- ✅ Accounting Standard

**사용 예시:**
```kotlin
val parser = Form20FParser()
val result = parser.parseHtml(htmlContent, metadata)

// 외국 기업 정보
println("Country: ${result.countryOfIncorporation}")
println("Accounting Standard: ${result.accountingStandard}") // "IFRS" or "US GAAP"
```

## 🔧 고급 사용법

### 파서 확장

새로운 보고서 타입 파서를 만들려면:

```kotlin
class FormXYZParser : BaseSecReportParser<FormXYZParseResult>(SecReportType.FORM_XYZ) {
    
    override fun parseHtml(htmlContent: String, metadata: SecReportMetadata): FormXYZParseResult {
        val cleanedContent = cleanHtml(htmlContent)
        // 파싱 로직 구현
        return FormXYZParseResult(...)
    }
    
    override fun parseText(textContent: String, metadata: SecReportMetadata): FormXYZParseResult {
        // 텍스트 파싱 로직 구현
        return FormXYZParseResult(...)
    }
    
    override fun extractSections(content: String): Map<String, String> {
        // 섹션 추출 로직 구현
    }
}
```

### 유틸리티 함수

```kotlin
// 보고서 타입별 중요도 점수
val score = SecReportParsingUtils.getReportImportanceScore(SecReportType.FORM_10K)
// Returns: 10

// 재무제표 포함 여부
val hasFinancials = SecReportParsingUtils.hasFinancialStatements(SecReportType.FORM_10K)
// Returns: true

// 감사된 재무제표 여부
val isAudited = SecReportParsingUtils.hasAuditedFinancials(SecReportType.FORM_10K)
// Returns: true

// 보고서 설명
val description = SecReportParsingUtils.getReportDescription(SecReportType.FORM_10K)
// Returns: "Annual report with comprehensive financial information..."
```

## 📈 통합 예제

### QuickAnalyze에서 사용

```kotlin
class FinancialAnalyzer {
    
    fun analyzeSecReport(
        htmlContent: String,
        formType: String,
        companyName: String,
        cik: String
    ): AnalysisResult {
        
        // 1. 메타데이터 생성
        val metadata = SecReportMetadata(
            formType = formType,
            companyName = companyName,
            cik = cik,
            filingDate = LocalDate.now().toString(),
            accessionNumber = ""
        )
        
        // 2. 적절한 파서로 파싱
        val parseResult = SecReportParsingUtils.parseReport(
            content = htmlContent,
            formType = formType,
            metadata = metadata
        )
        
        // 3. 타입별 분석
        return when (parseResult) {
            is Form10KParseResult -> analyze10K(parseResult)
            is Form10QParseResult -> analyze10Q(parseResult)
            is Form8KParseResult -> analyze8K(parseResult)
            else -> analyzeGeneric(parseResult)
        }
    }
    
    private fun analyze10K(result: Form10KParseResult): AnalysisResult {
        return AnalysisResult(
            summary = result.mdAndA?.executiveSummary ?: "",
            keyRisks = result.riskFactors.map { it.description },
            financialHighlights = extractFinancialHighlights(result.financialStatements),
            sections = result.sections
        )
    }
}
```

## 🎯 베스트 프랙티스

1. **타입 안전성**: 각 파서는 고유한 결과 타입을 반환하므로 타입 안전합니다
2. **확장성**: 새로운 보고서 타입 추가가 쉽습니다
3. **재사용성**: `BaseSecReportParser`의 공통 기능을 활용하세요
4. **에러 처리**: 섹션이 없을 수 있으므로 nullable 타입 사용

## 📚 참고 자료

- [SEC EDGAR Search](https://www.sec.gov/edgar/searchedgar/companysearch.html)
- [Form 10-K Guide](https://www.sec.gov/files/form10-k.pdf)
- [Form 10-Q Guide](https://www.sec.gov/files/form10-q.pdf)
- [Form 8-K Guide](https://www.sec.gov/files/form8-k.pdf)
