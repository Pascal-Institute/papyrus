package com.pascal.institute.ahmes.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** SEC API response models for ticker and filing information. */
@Serializable
data class TickerEntry(@SerialName("cik_str") val cik: Int, val ticker: String, val title: String)

@Serializable
data class SubmissionsRoot(
        val cik: String,
        val entityType: String? = null,
        val sic: String? = null,
        val sicDescription: String? = null,
        val name: String,
        val tickers: List<String> = emptyList(),
        val filings: Filings
)

@Serializable data class Filings(val recent: RecentFilings)

@Serializable
data class RecentFilings(
        val accessionNumber: List<String> = emptyList(),
        val filingDate: List<String> = emptyList(),
        val reportDate: List<String>? = null,
        val acceptanceDateTime: List<String> = emptyList(),
        val form: List<String> = emptyList(),
        val primaryDocument: List<String> = emptyList(),
        val primaryDocumentDescription: List<String>? = null
)

/** Simplified filing item for easier consumption. */
data class FilingItem(
        val accessionNumber: String,
        val filingDate: String,
        val form: String,
        val primaryDocument: String,
        val description: String
)
