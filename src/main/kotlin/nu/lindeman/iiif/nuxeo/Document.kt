package nu.lindeman.iiif.nuxeo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nu.lindeman.iiif.Nuxeo

@Serializable
data class Document (
    @SerialName("entity-type")
    val entitytype: String,
    val uid: String,
    val path: String,
    val title: String,
    val repository: String,
    val type: String,
    val state: String,
    val parentRef: String,
    val isCheckedOut: Boolean,
    val isVersion: Boolean,
    val isProxy: Boolean,
    val changeToken: String,
    val isTrashed: Boolean,
    val lastModified: String,
    val properties: Properties? = null
) {
    @Transient
    lateinit var nuxeo: Nuxeo

    @Serializable
    class Properties(
        @SerialName("dc:description")
        val dcDescription: String? = null,
        @SerialName("dc:title")
        val dcTitle: String? = null
    ) {
        @SerialName("picture:info")
        val pictureInfo : PictureInfo? = null
    }

    @Serializable
    class PictureInfo (val width: Int? = null, val height: Int? = null)

    fun setNuxeo(nuxeo: Nuxeo): Document {
        this.nuxeo = nuxeo
        return this
    }

    suspend fun parentFolder(): Document? {
        if (!this::nuxeo.isInitialized) throw Exception("set this.nuxeo first")
        return nuxeo.getDocument("uuid", this.parentRef, "Folder")
    }

}
