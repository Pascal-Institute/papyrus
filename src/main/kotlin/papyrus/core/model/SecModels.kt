package papyrus.core.model

// Re-export Ahmes models for backward compatibility
// This allows existing Papyrus code to continue using papyrus.core.model imports
// while the actual implementations live in the Ahmes library

typealias TickerEntry = com.pascal.institute.ahmes.model.TickerEntry

typealias SubmissionsRoot = com.pascal.institute.ahmes.model.SubmissionsRoot

typealias Filings = com.pascal.institute.ahmes.model.Filings

typealias RecentFilings = com.pascal.institute.ahmes.model.RecentFilings

typealias FilingItem = com.pascal.institute.ahmes.model.FilingItem
