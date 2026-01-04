# Papyrus - SEC Financial Analyzer

A powerful Kotlin Compose Desktop application for analyzing SEC financial reports with automatic metric extraction and AI-powered insights.

## ✨ Features

### 📊 **Intelligent Financial Analysis**

- **Drag & Drop Support**: Drop any SEC report (**PDF, HTML, HTM, TXT**) directly into the application
- **Automatic PDF Processing**: Seamless text extraction using Apache PDFBox
- **Key Metrics Extraction**: Automatically extracts critical financial data:
  - Revenue, Net Income, EPS (Earnings Per Share)
  - Total Assets, Liabilities, Equity
  - Cash Flow, Operating Income
  - And more!
- **Smart Parsing**: Intelligent detection of company name, report type (10-K, 10-Q, 8-K), and fiscal period

### 🤖 **AI-Powered Analysis** (Optional)

- **AI Financial Insights**: Comprehensive AI-generated analysis using OpenRouter API
- **Industry Comparisons**: Compare company performance against industry benchmarks
- **Investment Recommendations**: AI-driven investment suggestions based on financial health
- **Risk Assessment**: Automated evaluation of financial risks and opportunities
- **Key Insights**: AI-extracted critical takeaways from financial reports
- **Reanalysis Capability**: Re-run AI analysis on existing reports with one click
- **Smart Caching**: Skips redundant AI calls for already-analyzed documents

### 🏥 **Financial Health Score**

- **At-a-Glance Assessment**: Visual financial health scoring system (A+ to F)
- **Strengths & Weaknesses**: Clear identification of financial strong points and areas for improvement
- **Beginner-Friendly**: Simplified explanations designed for non-financial professionals
- **Actionable Recommendations**: Concrete suggestions based on financial analysis

### 🔍 **SEC EDGAR Integration**

- **Company Search**: Search by ticker symbol or company name across all SEC registered entities
- **Recent Filings Browser**: Browse recent filings (10-K, 10-Q, 8-K, DEF 14A, etc.)
- **Direct Browser Access**: Open filings directly in SEC EDGAR website
- **Quick Analysis**: Instant text analysis with keyword detection
- **Bookmark Management**: Save favorite companies for quick access
- **Recent Views**: Track recently viewed companies and filings

### 🎨 **Modern User Experience**

- **Clean Interface**: Emoji-free, professional UI design following AGENTS.md principles
- **Intuitive Navigation**: Tab-based analysis view with clear categorization
- **Real-time Updates**: Live status updates during document processing
- **Error Handling**: Graceful error messages with retry capabilities

## 📋 How to Use

### 1. **Analyze a Downloaded SEC Report**

1. Download any SEC filing (HTML, PDF, TXT) from [SEC EDGAR](https://www.sec.gov/edgar/search/)
2. Launch Papyrus
3. **Drag and drop** the file onto the right panel (or click "Browse Files")
4. View automatically extracted financial metrics with formatted values
5. Review AI analysis (if configured) including insights, recommendations, and risk assessment
6. Check the Financial Health Score for a quick understanding
7. Close analysis to return to the main screen

### 2. **Search and Browse SEC Filings**

1. Type a company name or ticker in the search box (left panel)
2. Select a company from the results
3. Browse their recent filings with date and type information
4. Click **"Quick Analyze"** to analyze a filing with AI insights
5. Click **"Open Browser"** to view the original document on SEC website
6. Bookmark companies for quick access later

### 3. **Configure AI Analysis** (Optional)

1. Click the **Settings** icon (⚙️) in the top right
2. Enter your OpenRouter API key
3. Save the configuration
4. AI analysis will automatically run on all future document analyses
5. Use the **"Reanalyze with AI"** button to add AI insights to existing analyses

> **Note**: AI analysis is optional. The app provides comprehensive financial analysis even without AI configuration.

## 🏗️ Project Structure

The codebase follows AGENTS.md principles: **intuitive, concise, and meaningful**.

```
src/main/kotlin/papyrus/
├── Main.kt                               # Main application entry point with UI orchestration
├── core/
│   ├── model/
│   │   ├── BookmarkModels.kt            # Bookmark and recently viewed data models
│   │   ├── FinancialModels.kt           # Financial analysis result models
│   │   ├── NewsModels.kt                # Company news data models
│   │   ├── ParserModels.kt              # Financial statement parsing models
│   │   └── SecModels.kt                 # SEC EDGAR API response models
│   ├── network/
│   │   ├── SecApi.kt                    # SEC EDGAR API client (Ktor HTTP)
│   │   └── NewsApi.kt                   # Financial news API client
│   └── service/
│       ├── AiAnalysisService.kt         # OpenRouter AI integration for financial analysis
│       ├── EnhancedFinancialParser.kt   # Advanced financial statement parser
│       └── FinancialAnalyzer.kt         # Core financial analysis engine
├── ui/
│   ├── AppTheme.kt                       # Material Design theme and colors
│   ├── Components.kt                     # Reusable UI components (cards, lists, etc.)
│   ├── DragDropPanel.kt                  # File drag & drop interface
│   ├── QuickAnalyzeView.kt              # Financial analysis results display
│   └── SettingsDialog.kt                 # AI API key configuration dialog
└── util/
    ├── BookmarkManager.kt                # Bookmark and recent views persistence
    ├── FileUtils.kt                      # File type detection and text extraction
    ├── PdfParser.kt                      # PDF document parser (Apache PDFBox)
    └── SettingsManager.kt                # Application settings management
```

## 🚀 How to Run

### Prerequisites

- **Java 17 or higher** (JDK 17+)
- **Internet connection** (for SEC API and AI features)
- **OpenRouter API Key** (optional, for AI analysis features)

### Using Gradle Wrapper (Recommended)

**On Windows:**

```bash
.\gradlew run
```

**On macOS/Linux:**

```bash
./gradlew run
```

### Building a Distributable Package

Create a native application package:

```bash
# Windows
.\gradlew packageDistributionForCurrentOS

# macOS/Linux
./gradlew packageDistributionForCurrentOS
```

The built application will be in `build/compose/binaries/main/` directory.

## ⚙️ Configuration

### AI Analysis Setup (Optional)

1. **Get an OpenRouter API Key**:

   - Visit [OpenRouter](https://openrouter.ai/)
   - Sign up for a free account
   - Generate an API key from the dashboard

2. **Configure in Papyrus**:

   - Click the Settings icon (⚙️) in the top right
   - Paste your API key
   - Click "Save"

3. **Alternative**: Set environment variable:
   ```bash
   export OPENROUTER_API_KEY="your-api-key-here"
   ```

### Settings Location

Application settings are stored in:

- **Windows**: `%USERPROFILE%\.papyrus\settings.properties`
- **macOS/Linux**: `~/.papyrus/settings.properties`

## 🛠️ Technologies

- **Kotlin** 1.9+
- **Compose Multiplatform** for Desktop UI
- **Ktor** for HTTP API clients
- **Apache PDFBox** for PDF text extraction
- **Kotlinx Serialization** for JSON parsing
- **OpenRouter API** for AI-powered analysis
- **Gradle** build system

## 📝 Development Principles

This project follows the principles outlined in [AGENTS.md](AGENTS.md):

1. **Intuitive**: Code that is obvious to anyone reading it
2. **Concise**: Minimal boilerplate, clear intent
3. **Meaningful**: Self-documenting names and structure
4. **Financial Precision**: Accurate financial calculations using `BigDecimal` where necessary

## 🤝 Contributing

Contributions are welcome! Please ensure your code follows the AGENTS.md principles:

- Clear, self-explanatory variable and function names
- Minimal abstraction unless necessary
- Comments only when the code cannot be self-documenting

## 📄 License

This project is open source and available under the MIT License.

## 🔗 Resources

- [SEC EDGAR Search](https://www.sec.gov/edgar/search/)
- [OpenRouter AI](https://openrouter.ai/)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

## 📧 Support

For issues, questions, or feature requests, please open an issue on GitHub.

---

**Built with ❤️ using Kotlin and Compose Multiplatform**

```powershell
.\gradlew.bat run
```

### Using IntelliJ IDEA

1. Open the `c:\papyrus` folder as a project
2. Wait for Gradle sync to complete
3. Click the green ▶️ Run button in the toolbar
4. Or run from terminal: `.\gradlew.bat run`

## Technical Details

- **Framework**: Jetpack Compose for Desktop (v1.6.1)
- **Language**: Kotlin 1.9.23
- **HTTP Client**: Ktor 2.3.7
- **Serialization**: kotlinx.serialization
- **Drag & Drop**: Native Java AWT integration

## Notes

- The financial analyzer uses pattern matching to extract metrics from HTML documents
- Accuracy depends on document structure and formatting
- For best results, use official SEC EDGAR HTML filings
- User-Agent is configured for SEC compliance; update with your contact info for production use

## License

This is a demonstration project for educational purposes.
