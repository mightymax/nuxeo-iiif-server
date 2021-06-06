package nu.lindeman.iiif

import com.typesafe.config.Config
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nu.lindeman.iiif.nuxeo.Document
import nu.lindeman.iiif.nuxeo.Documents

@Serializable
class Manifest
    constructor(
        @Transient
        var config: Config? = null,

        @Transient
        var folder: Document? = null,

        @Transient
        var pictures: Documents? = null
    )
{
    var id: String
    var type: String
    var context: String
    var label: Label?
    var thumbnail: Thumbnail
    var items: Array<CanvasItem> = emptyArray()

    init {
        context = "http://iiif.io/api/presentation/3/context.json"
        id = "${config?.getString("prefix")}/manifest/folder/${folder?.uid}"
        type = "Manifest"
        label = Label(folder?.properties?.dcTitle!!)
        thumbnail = Thumbnail(pictures!!.entries[0])
        items = Array<CanvasItem>(pictures!!.currentPageSize) { i ->
            CanvasItem(pictures!!.entries[i], config)
        }
    }

    @Serializable
    class Label (@Transient val label: String = "") {
        val none: Array<String> = arrayOf(label)
    }

    @Serializable
    class CanvasItem(
        @Transient val picture: Document? = null,
        @Transient val config: Config? = null
    ) {
        val id: String
        val type: String
        val label: Label
        val height: Int
        val width: Int
        var items: Array<AnnotationPage> = emptyArray()

        init {
            id = "${config?.getString("prefix")}/${picture?.uid}/canvas"
            type = "Canvas"
            label = Label(picture?.properties?.dcTitle.toString())
            height = picture?.properties?.pictureInfo?.height!!
            width = picture.properties.pictureInfo.width!!
            items = Array<AnnotationPage>(1) { _ ->
                AnnotationPage(picture, config)
            }
        }

        @Serializable
        class AnnotationPage(
            @Transient val picture: Document? = null,
            @Transient val config: Config? = null
        ) {
            val id: String
            val type: String
            var items: Array<Annotation> = emptyArray()

            init {
                id = "${config?.getString("prefix")}/${picture?.uid}/canvas/page"
                type = "AnnotationPage"
                items = Array<Annotation>(1){_-> Annotation(picture, config) }
            }

            @Serializable
            class Annotation(
                @Transient val picture: Document? = null,
                @Transient val config: Config? = null
            ) {
                val id: String
                val type: String
                val motivation: String
                val target: String
                val body: Body

                init {
                    id = "${config?.getString("prefix")}/${picture?.uid}/canvas/page"
                    type = "Annotation"
                    motivation = "painting"
                    target = "${config?.getString("prefix")}/${picture?.uid}/canvas"
                    body = Body(picture, config)
                }

                @Serializable
                class Body (
                    @Transient val picture: Document? = null,
                    @Transient val config: Config? = null
                ){
                    val id: String
                    val type: String
                    val width: Int
                    val height: Int
                    val service: Thumbnail.Service

                    init {
                        id = "${config?.getString("prefix")}/${picture?.uid}/full/max/0/default.jpg"
                        type = "Image"
                        width = picture?.properties?.pictureInfo?.width ?: 0
                        height = picture?.properties?.pictureInfo?.height ?: 0
                        service = Thumbnail.Service(picture, config)
                    }
                }
            }
        }
    }

    @Serializable
    class Thumbnail (
        @Transient var picture: Document? = null,
        @Transient val config: Config? = null
    ) {
        val id: String
        val type: String
        val format: String
        val service: Service

        @Serializable
        class Service(
            @Transient var picture: Document? = null,
            @Transient val config: Config? = null
        ) {
            val id: String
            val type: String
            val profile: String

            init {
                id = "${config?.getString("prefix")}/${picture?.uid}"
                type = "ImageService3"
                profile = "level2"
            }
        }

        init {
            id = "${config?.getString("prefix")}/${picture?.uid}/full/500,/0/default.jpg"
            type = "Image"
            format = "image/jpeg"
            service = Service(picture)
        }
    }
}

