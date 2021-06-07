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
    val numberOfPages: Int = 0,
    val resultsCount: Int = 0,
    val isPaginable: Boolean = true,
    val pageSize: Int = 100,
    val maxPageSize: Int = 100,
    val currentPageSize: Int = 0,
    val currentPageIndex: Int = 0,
    val isPreviousPageAvailable: Boolean = false,
    val isNextPageAvailable: Boolean = false,
    val isLastPageAvailable: Boolean = false,
    val isSortable: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val totalSize: Int = 0,
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val entries: MutableList<Document> = mutableListOf()
)
