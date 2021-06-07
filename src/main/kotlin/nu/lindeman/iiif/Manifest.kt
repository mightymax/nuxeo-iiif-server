package nu.lindeman.iiif

import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import nu.lindeman.iiif.nuxeo.Document
import nu.lindeman.iiif.nuxeo.Documents

@Serializable
class Manifest
    constructor(
        @Transient
        var iiifUrl: URLBuilder = URLBuilder("http://localhost/iiif/3"),

        @Transient
        var folder: Document = Document(),

        @Transient
        var pictures: Documents = Documents()
    )
{

    @Transient
    var baseUrl = iiifUrl.clone().path("manifest/from/${folder.type.lowercase()}/uid/${folder.uid}")

    private var id: String
    private var type: String
    private var context: String
    private var label: Label?
    private var thumbnail: Thumbnail
    private var items: Array<CanvasItem> = emptyArray()

    init {
        id = baseUrl.buildString()
        context = "http://iiif.io/api/presentation/3/context.json"
        type = "Manifest"
        label = Label(folder.properties?.dcTitle!!)
        thumbnail = Thumbnail(pictures.entries[0])
        items = Array(pictures.currentPageSize) { i ->
            CanvasItem(pictures.entries[i])
        }
    }

    @Serializable
    inner class Label (@Transient val label: String = "") {
        val none: Array<String> = arrayOf(label)
    }

    @Serializable
    inner class CanvasItem(
        @Transient val picture: Document = Document()
    ) {
        private val id: String
        private val type: String
        private val label: Label
        private var height: Int = 0
        private var width: Int = 0
        private var items: Array<AnnotationPage> = emptyArray()

        init {
            id = "${baseUrl.buildString()}/Canvas/${picture.uid}"
            type = "Canvas"
            label = Label(picture.properties?.dcTitle.toString())
            picture.properties?.pictureInfo?.let{
                width = it.width
                height = it.height
            }
            items = Array<AnnotationPage>(1) { _ ->
                AnnotationPage()
            }
        }

        @Serializable
        inner class AnnotationPage() {
            private val id: String
            private val type: String
            private var items: Array<Annotation> = emptyArray()

            init {
                id = "${baseUrl.buildString()}/Canvas/AnnotationPage/${this@CanvasItem.picture.uid}"
                type = "AnnotationPage"
                items = Array(1){ Annotation() }
            }

            @Serializable
            inner class Annotation() {
                private val id: String
                private val type: String
                private val motivation: String
                private val target: String
                private val body: Body

                init {
                    id = "${baseUrl.buildString()}/Canvas/AnnotationPage/Annotation/${this@CanvasItem.picture.uid}"
                    type = "Annotation"
                    motivation = "painting"
                    target = "${baseUrl.buildString()}/Canvas/${this@CanvasItem.picture.uid}"
                    body = Body()
                }

                @Serializable
                inner class Body () {
                    private val id: String
                    private val type: String
                    private var width: Int = 0
                    private var height: Int = 0
                    private val service: Service

                    init {
                        id = "${iiifUrl.buildString()}/${this@CanvasItem.picture.uid}/full/500,/0/default.jpg"
                        type = "Image"
                        this@CanvasItem.picture.properties?.pictureInfo?.let{
                            width = it.width
                            height = it.height
                        }
                        service = Service(this@CanvasItem.picture)
                    }
                }
            }
        }
    }

    @Serializable
    inner class Thumbnail (
        @Transient var picture: Document = Document()
    ) {
        private val id: String
        private val type: String
        private val format: String
        private val service: Service


        init {
            id = "${iiifUrl.buildString()}/${picture.uid}/full/500,/0/default.jpg"
            type = "Image"
            format = "image/jpeg"
            service = Service(picture)
        }
    }

    @Serializable
    inner class Service(
        @Transient var picture: Document = Document()
    ) {
        private val id: String
        private val type: String
        private val profile: String

        init {
            id = "${iiifUrl.buildString()}/${picture.uid}"
            type = "ImageService3"
            profile = "level2"
        }
    }
}

