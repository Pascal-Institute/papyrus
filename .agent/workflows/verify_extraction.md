---
description: Verify that the financial data extraction logic is working correctly by running the parser tests.
---

To verify `HtmlParser` and `EnhancedFinancialParser` logic:

1. Run the specific test class using Gradle:
   // turbo
   ```bash
   .\gradlew.bat test --tests "papyrus.HtmlParserTest"
   ```

2. If you need to run all tests:
   ```bash
   .\gradlew.bat test
   ```

3. Check the report at `build/reports/tests/test/index.html` (if running locally) or check the console output (if running through agent).
