# Workspace Configuration Documentation

## Overview

This workspace is configured for optimal SEC financial report analysis and investment insight derivation, following the principles outlined in [AGENTS.md](../AGENTS.md).

## VS Code Configuration

### Settings ([.vscode/settings.json](.vscode/settings.json))

-   **Encoding**: UTF-8 for all files
-   **Auto-save**: Enabled with 1s delay
-   **Format on save**: Enforces code consistency
-   **File associations**: SEC reports (`.htm`, `.xbrl`, `.xsd`)
-   **Search exclusions**: Build artifacts, caches
-   **Kotlin language server**: Enabled for better code intelligence

### Extensions ([.vscode/extensions.json](.vscode/extensions.json))

Recommended extensions for financial analysis development:

-   **Kotlin**: Language support (mathiasfrohlich.kotlin, fwcd.kotlin)
-   **Java/Gradle**: Build tools support
-   **XML**: SEC XBRL report parsing
-   **CSV**: Financial data inspection
-   **Better Comments**: Highlighting FINANCIAL and SEC tags

### Tasks ([.vscode/tasks.json](.vscode/tasks.json))

Quick access to common operations:

-   `Ctrl+Shift+B`: Default build task
-   **Gradle: Clean Build**: Full rebuild
-   **Gradle: Test**: Run all tests
-   **Gradle: Analyze SEC Report**: Run financial analyzer tests
-   **Gradle: Run Application**: Launch desktop app

### Launch Configuration ([.vscode/launch.json](.vscode/launch.json))

-   Debug main application
-   Attach to running JVM for advanced debugging

## Code Style

### EditorConfig ([.editorconfig](../.editorconfig))

Enforces consistent coding style across the team:

-   **Indentation**: 4 spaces for Kotlin/Java
-   **Line endings**: LF (Unix style)
-   **Max line length**: 120 characters
-   **Final newline**: Always insert
-   **Trim whitespace**: Automatic

### Kotlin Style Guidelines

Per AGENTS.md principles:

1. **Intuitive**: Readable over clever
2. **Concise**: Functions < 30 lines
3. **Meaningful names**: Domain-specific (e.g., `extractRevenueFromSection`, `calculateROI`)
4. **Financial precision**: Use `BigDecimal` and JavaMoney for calculations

## Financial Precision Standards

Following AGENTS.md Principle 4:

-   **Never use `Double` or `Float`** for money calculations
-   **Use `BigDecimal`** with explicit rounding modes
-   **Use JavaMoney** (Moneta) for currency-aware operations
-   **Validate financial logic** with sanity checks

Example:

```kotlin
// ❌ WRONG
val profit = revenue * 0.22

// ✅ CORRECT
val taxRate = BigDecimal("0.22")
val profit = revenue.multiply(taxRate).setScale(2, RoundingMode.HALF_UP)
```

## SEC Report Analysis

### Sample Reports

Located in [src/main/resources/samples/](../src/main/resources/samples/):

-   `joby-20220930.htm`: Reference for data extraction patterns

### When uncertain about extraction:

1. Consult actual SEC reports in `/samples`
2. Model code based on real data structures
3. Ensure high-value information extraction

## Testing

Run tests with:

```bash
./gradlew test
```

View results:

-   Terminal output
-   HTML report: `build/reports/tests/test/index.html`

## Comments

Per AGENTS.md: **All comments must be in English**

Use special tags for financial context:

```kotlin
// FINANCIAL: BigDecimal used to prevent rounding errors in ROI calculation
// SEC: Extracting 'us-gaap:Revenues' from XBRL taxonomy
```

## Git Workflow

`.gitignore` is configured to:

-   ✅ Track `.vscode` configuration for team consistency
-   ❌ Exclude build artifacts, logs, IDE files
-   ❌ Exclude local properties files

## Mission Alignment

Everything in this workspace serves the mission:

> **Derive investment insights from SEC filings**

Before adding any feature or configuration:

1. Does it improve **accuracy**?
2. Does it enhance **clarity**?
3. Does it ensure **completeness**?

If not, reconsider.

---

**Last Updated**: January 7, 2026
**Maintained by**: AI Development Agent following AGENTS.md principles
