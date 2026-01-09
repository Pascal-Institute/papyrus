# SEC Network API Migration to Ahmes

## Overview
The SEC network API client (`SecApi`) has been migrated from Papyrus to the Ahmes library to improve code reusability and maintain a single source of truth for all SEC data access.

## What Was Moved

### 1. **Models** → `ahmes/src/main/kotlin/com/pascal/institute/ahmes/model/SecApiModels.kt`
- `TickerEntry`
- `SubmissionsRoot`
- `Filings`
- `RecentFilings`
- `FilingItem`

### 2. **Network Client** → `ahmes/src/main/kotlin/com/pascal/institute/ahmes/network/SecApiClient.kt`
- Generic SEC API client with configurable settings
- All SEC API endpoints:
  - Ticker search
  - Submissions/Filings
  - Company Facts (XBRL)
  - Document URLs and content fetching

### 3. **Papyrus Wrapper** → `papyrus/src/main/kotlin/papyrus/core/network/SecApi.kt`
- Lightweight wrapper that delegates to Ahmes client
- App-specific configuration (user agent, contact email)
- Maintains existing API for backward compatibility

## Architecture

```
┌─────────────────────────────────────┐
│        Papyrus Application          │
│                                     │
│  ┌─────────────────────────────┐   │
│  │   SecApi (Wrapper)          │   │
│  │  - PapyrusApp/1.0 config    │   │
│  └──────────┬──────────────────┘   │
│             │                       │
└─────────────┼───────────────────────┘
              │ delegates to
              ▼
┌─────────────────────────────────────┐
│         Ahmes Library               │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  SecApiClient (Generic)     │   │
│  │  - Configurable user agent  │   │
│  │  - Rate limiting            │   │
│  │  - All SEC endpoints        │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  SEC API Models             │   │
│  │  - TickerEntry              │   │
│  │  - SubmissionsRoot          │   │
│  │  - CompanyFacts             │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

## Benefits

### ✅ **Code Reusability**
- Other projects can use Ahmes to access SEC data without duplicating code
- Single, well-tested implementation

### ✅ **Separation of Concerns**
- Ahmes = Generic SEC data access library
- Papyrus = Application-specific wrapper with custom configuration

### ✅ **Easier Maintenance**
- Bug fixes and improvements in one place
- Consistent behavior across all projects using Ahmes

### ✅ **Flexibility**
- Each app can configure user agent, rate limits, etc.
- Ahmes client is not opinionated about these settings

## Configuration

The `SecApiClient` accepts a `SecApiConfig`:

```kotlin
SecApiConfig(
    userAgent = "YourApp/1.0",
    contactEmail = "contact@yourapp.com",
    rateLimitDelayMs = 100L  // Delay between requests
)
```

## Backward Compatibility

### For Papyrus Code
All existing Papyrus code continues to work without changes:

```kotlin
// Still works!
import papyrus.core.model.*
import papyrus.core.network.SecApi

val tickers = SecApi.searchTicker("AAPL")
val submissions = SecApi.getSubmissions(cik)
```

The `papyrus.core.model.SecModels.kt` now uses `typealias` to redirect to Ahmes models:

```kotlin
typealias TickerEntry = com.pascal.institute.ahmes.model.TickerEntry
typealias SubmissionsRoot = com.pascal.institute.ahmes.model.SubmissionsRoot
// etc.
```

### For New Code
New code can import directly from Ahmes:

```kotlin
import com.pascal.institute.ahmes.model.*
import com.pascal.institute.ahmes.network.*

val client = SecApiClient(SecApiConfig(...))
val tickers = client.searchTicker("MSFT")
```

## Dependencies Added to Ahmes

```kotlin
// HTTP Client (Ktor)
implementation("io.ktor:ktor-client-core:2.3.7")
implementation("io.ktor:ktor-client-cio:2.3.7")
implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
```

## Migration Summary

| Component | Before | After |
|-----------|--------|-------|
| Models | `papyrus.core.model.SecModels.kt` | `ahmes.model.SecApiModels.kt` (with typealias in Papyrus) |
| Network Client | `papyrus.core.network.SecApi.kt` (213 lines) | `ahmes.network.SecApiClient.kt` (generic) + `papyrus.core.network.SecApi.kt` (wrapper, 45 lines) |
| Dependencies | Papyrus only | Ahmes (shared) |
| Reusability | Papyrus-specific | Available to all projects |

## Testing

After migration, run:
```bash
./gradlew compileKotlin  # Verify compilation
./gradlew test           # Run tests
```

✅ All builds passing!
