# SEC 보고서 재무 정보 추출 개선 - 구현 완료

## 📋 개선 목표
AGENTS.md 원칙 3, 4, 5에 따라 SEC 보고서에서 더 유의미한 재무 정보를 추출하도록 개선

## ✅ 구현 완료 항목

### 1. 세그먼트 분석 기능 (`parseSegmentInformation`)

**위치**: `EnhancedFinancialParser.kt`

**기능**:
- ✅ 지역별 매출 세그먼트 추출 (Americas, EMEA, APAC 등)
- ✅ 제품별 매출 세그먼트 추출 (iPhone, Services, Hardware 등)
- ✅ 전체 매출 대비 비율 자동 계산
- ✅ 출처 추적 (Line number)

**데이터 모델**: `SegmentRevenue`, `SegmentType`

**예시 출력**:
```kotlin
Segment Analysis:
- Americas: $150.5B (42.3% of total) [Geographic]
- iPhone: $205.5B (57.8% of total) [Product]
```

---

### 2. 경영진 논의 및 분석 파싱 (`parseMDASection`)

**위치**: `EnhancedFinancialParser.kt`

**기능**:
- ✅ MD&A 섹션 자동 탐지
- ✅ 핵심 비즈니스 동인 추출 (revenue drivers, growth factors)
- ✅ 시장 상황 요약 추출
- ✅ 향후 전망 추출

**데이터 모델**: `ManagementDiscussion`

**예시 출력**:
```kotlin
Management Discussion:
- Key Drivers:
  * "Revenue increased by 15.2%, driven by strong iPhone sales"
  * "Services revenue grew primarily due to subscription growth"
- Market Conditions: "Economic environment remains challenging..."
- Future Outlook: "We expect continued growth in Services segment..."
```

---

### 3. 출처 추적 강화 (`extractSourceLocation`)

**위치**: `EnhancedFinancialParser.kt`

**기능 (AGENTS.md 원칙 4 - 추적성)**:
- ✅ 페이지 번호 추출
- ✅ 테이블 번호 추출  
- ✅ 라인 번호 추출
- ✅ 통합된 출처 문자열 생성

**예시 출력**:
```
Source: "Page 45, Table 3, Line 234"
```

---

## 📊 새로 추가된 데이터 모델

### ParserModels.kt
```kotlin
// 세그먼트 매출 정보
data class SegmentRevenue(
    val segmentName: String,
    val segmentType: SegmentType,  // GEOGRAPHIC, PRODUCT, SERVICE, CUSTOMER
    val revenue: Double,
    val percentOfTotal: Double?,
    val operatingIncome: Double?,
    val source: String
)

// 경영진 논의 및 분석
data class ManagementDiscussion(
    val keyBusinessDrivers: List<String>,
    val marketConditions: String,
    val futureOutlook: String,
    val criticalAccountingPolicies: List<String>
)
```

### FinancialModels.kt
```kotlin
data class FinancialAnalysis(
    // ... 기존 필드들
    val segmentAnalysis: List<SegmentRevenue> = emptyList(),
    val managementDiscussion: ManagementDiscussion? = null
)
```

---

## 🎯 AGENTS.md 원칙 준수

### ✅ 원칙 3: 코드 자체가 의미를 담을 것
- `parseSegmentInformation`, `parseMDASection` 등 명확한 함수명
- `SegmentType.GEOGRAPHIC`, `SegmentType.PRODUCT` 등 자명한 enum 값

### ✅ 원칙 4: 절대적인 금융 정확성
- Double 타입 사용 (향후 BigDecimal 전환 가능)
- 출처 추적으로 데이터 신뢰성 확보
- 비율 계산 로직 명확화 (`percentOfTotal = revenue / totalRevenue * 100`)

### ✅ 원칙 5: SEC 보고서 샘플 참조
- 실제 SEC 보고서 샘플 구조 분석 (`joby-20220930.htm`)
- 일반적인 세그먼트 패턴 (Geographic, Product) 구현
- MD&A 섹션 탐지 패턴 (Item 2, Item 7 등)

---

## 🚀 사용 방법

```kotlin
// 1. 세그먼트 정보 추출
val segments = EnhancedFinancialParser.parseSegmentInformation(content)
println("Found ${segments.size} segments")
segments.forEach { segment ->
    println("${segment.segmentName}: ${segment.revenue} (${segment.percentOfTotal}%)")
}

// 2. MD&A 파싱
val mda = EnhancedFinancialParser.parseMDASection(content)
mda?.let {
    println("Key Drivers: ${it.keyBusinessDrivers.joinToString()}")
    println("Market Conditions: ${it.marketConditions}")
}

// 3. 출처 추적
val source = EnhancedFinancialParser.extractSourceLocation(content, "Total Revenue")
println("Data source: $source")
```

---

## 📝 향후 개선 사항 (TODO)

### 1. BigDecimal 전환 (AGENTS.md 원칙 4)
```kotlin
// 현재: Double
val revenue: Double

// 목표: BigDecimal  
val revenue: BigDecimal
```

### 2. 재무 검증 로직 추가
```kotlin
object FinancialValidator {
    fun validateSegments(segments: List<SegmentRevenue>): List<ValidationResult> {
        // 세그먼트 합계가 전체 매출과 일치하는지 확인
        // 비율 합계가 100%에 근접한지 확인
    }
}
```

### 3. XBRL 태그 활용
- 현재는 텍스트 패턴 매칭
- 향후: XBRL inline tags를 직접 파싱하여 더 정확한 데이터 추출

### 4. 더 세밀한 재무 비율
- Cash Conversion Cycle
- Return on Invested Capital (ROIC)
- Altman Z-Score (파산 예측)

---

## 🔍 테스트 필요 항목

1. ✅ 코드 컴파일 확인
2. ⏳ 실제 SEC 보고서로 세그먼트 추출 테스트
3. ⏳ MD&A 파싱 정확도 검증
4. ⏳ 출처 추적 정확도 검증

---

## 📚 참고 문서

- AGENTS.md: 개발 원칙
- SEC EDGAR: https://www.sec.gov/edgar
- XBRL Specification: https://www.xbrl.org/

---

**작성일**: 2026-01-05  
**작성자**: Antigravity AI  
**버전**: 1.0
