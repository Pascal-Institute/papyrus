package papyrus.core.model

import kotlinx.serialization.Serializable

@Serializable
data class NewsArticle(
        val title: String,
        val url: String,
        val source: String,
        val publishedAt: String,
        val description: String? = null,
        val imageUrl: String? = null
)

data class CompanyNews(
        val ticker: String,
        val companyName: String,
        val articles: List<NewsArticle>,
        val lastUpdated: String
)
