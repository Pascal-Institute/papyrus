package papyrus.core.model

import com.pascal.institute.ahmes.model.FilingItem as AhmesFilingItem
import com.pascal.institute.ahmes.model.Filings as AhmesFilings
import com.pascal.institute.ahmes.model.RecentFilings as AhmesRecentFilings
import com.pascal.institute.ahmes.model.SubmissionsRoot as AhmesSubmissionsRoot
import com.pascal.institute.ahmes.model.TickerEntry as AhmesTickerEntry

fun AhmesTickerEntry.toPapyrus(): TickerEntry =
        TickerEntry(cik = this.cik, ticker = this.ticker, title = this.title)

fun AhmesSubmissionsRoot.toPapyrus(): SubmissionsRoot =
        SubmissionsRoot(
                cik = this.cik,
                entityType = this.entityType,
                sic = this.sic,
                sicDescription = this.sicDescription,
                name = this.name,
                tickers = this.tickers,
                filings = this.filings.toPapyrus()
        )

fun AhmesFilings.toPapyrus(): Filings = Filings(recent = this.recent.toPapyrus())

fun AhmesRecentFilings.toPapyrus(): RecentFilings =
        RecentFilings(
                accessionNumber = this.accessionNumber,
                filingDate = this.filingDate,
                reportDate = this.reportDate,
                acceptanceDateTime = this.acceptanceDateTime,
                form = this.form,
                primaryDocument = this.primaryDocument,
                primaryDocumentDescription = this.primaryDocumentDescription
        )

fun AhmesFilingItem.toPapyrus(): FilingItem =
        FilingItem(
                accessionNumber = this.accessionNumber,
                filingDate = this.filingDate,
                form = this.form,
                primaryDocument = this.primaryDocument,
                description = this.description
        )
