package nu.lindeman.iiif.nuxeo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.Any

@Serializable
data class Documents(
    @SerialName("entity-type")
    val entitytype: String? = null,
    val status: Int? = null,
    val message: String? = null,
    val numberOfPages: Int,
    val resultsCount: Int,
    val isPaginable: Boolean,
    val pageSize: Int,
    val maxPageSize: Int,
    val currentPageSize: Int,
    val currentPageIndex: Int,
    val isPreviousPageAvailable: Boolean,
    val isNextPageAvailable: Boolean,
    val isLastPageAvailable: Boolean,
    val isSortable: Boolean,
    val hasError: Boolean,
    val errorMessage: String? = null,
    val totalSize: Int,
    val pageIndex: Int,
    val pageCount: Int,
    val entries: MutableList<Document> = mutableListOf()
)
