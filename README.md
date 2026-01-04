# Papyrus - SEC Financial Analyzer

A powerful Kotlin Compose Desktop application for analyzing SEC financial reports with automatic metric extraction.

## Features

### 📊 **Drag & Drop Financial Analysis**
- Drop any SEC report (**PDF, HTML, HTM, TXT**) directly into the application
- **Automatic PDF text extraction** using Apache PDFBox
- Automatic extraction of key financial metrics:
  - Revenue, Net Income, EPS
  - Total Assets, Liabilities, Equity
  - And more!
- Intelligent parsing of company name, report type (10-K, 10-Q), and period
- Beautiful summary view with emoji indicators

### 🔍 **SEC EDGAR Search**
- Search companies by Ticker symbol or company name
- Browse recent filings (10-K, 10-Q, 8-K, etc.)
- View filings directly in your browser
- Quick text analysis with keyword detection

## How to Use

### 1. **Analyze a Downloaded SEC Report**
1. Download any SEC filing as HTML from [SEC EDGAR](https://www.sec.gov/edgar/search/)
2. Launch Papyrus
3. **Drag and drop** the HTML file onto the right panel (or click "Browse Files")
4. View automatically extracted financial metrics with formatted values
5. Close analysis to return to the main screen

### 2. **Search and Browse SEC Filings**
1. Type a company name or ticker in the search box
2. Select a company from the results
3. Browse their recent filings
4. Click "Open Browser" to view in SEC website
5. Click "Quick Analyze" for a text preview

## Project Structure
```
src/main/kotlin/papyrus/
├── Main.kt              # Main UI with drag & drop support
├── SecApi.kt            # SEC EDGAR API client (Ktor)
├── Models.kt            # SEC data models (@Serializable)
└── FinancialData.kt     # Financial metrics extraction engine
```

## How to Run

### Using Gradle Wrapper (Recommended)
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
