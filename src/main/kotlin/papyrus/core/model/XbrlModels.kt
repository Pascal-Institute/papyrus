package papyrus.core.model

import kotlinx.serialization.Serializable

@Serializable
data class XbrlCompanyFact(
    val concept: String,
    val label: String,
    val unit: String? = null,
    val periodEnd: String? = null,
    val value: Double? = null,
)
