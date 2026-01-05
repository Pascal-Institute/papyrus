# Parser Design Documentation

## Overview
The parser system has been refactored into format-specific classes for better maintainability and separation of concerns.

## Architecture

### Core Interface
- `DocumentParser.kt` - Base interface that all parsers implement

### Format-Specific Parsers
1. **HtmlParser.kt** - HTML/HTM files
   - Cleans HTML tags and XBRL data
   - Decodes HTML entities
   - Detects XBRL and table structures

2. **PdfParser.kt** - PDF files  
   - Uses Apache PDFBox for binary PDF extraction
   - Handles SEC viewer HTML responses
   - Parses table structures based on alignment

3. **TxtParser.kt** - Plain text files
   - Processes SEC submission text format
   - Extracts sections from `<DOCUMENT>` tags
   - Normalizes whitespace

### Factory
- `ParserFactory.kt` - Creates appropriate parser based on:
  - File extension
  - Content analysis
  - Auto-detection

## Usage

```kotlin
// By extension
val parser = ParserFactory.getParserByExtension("pdf")
val result = parser.parse(content, "10-K.pdf")

// By content auto-detection  
val result = ParserFactory.parseDocument(content, "report.htm")

// Get all supported extensions
val extensions = ParserFactory.getSupportedExtensions() // [pdf, html, txt]
```

##  Integration Status

⚠️ **Note**: The new parser classes are created but not yet integrated with the main application.

### Next Steps:
1. Update FinancialAnalyzer to use ParserFactory
2. Connect file format selection UI to appropriate parsers
3. Add unit tests for each parser
4. Add PDF

Box dependency to build.gradle.kts

## Benefits

✅ **Separation of Concerns** - Each parser handles one format  
✅ **Easier Maintenance** - Format-specific logic is isolated  
✅ **Extensibility** - Easy to add new formats  
✅ **Testability** - Each parser can be tested independently
