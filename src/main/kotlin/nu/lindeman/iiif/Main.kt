package nu.lindeman.iiif

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.TextContent
import io.ktor.features.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    val config: Config = ConfigFactory.load()
    val nuxeo = Nuxeo(config.getConfig("app.nuxeo"))

    var serverAddressUrl: URLBuilder

    (environment as ApplicationEngineEnvironment).connectors[0].let{
        serverAddressUrl = URLBuilder("${it.type.name}://${it.host}:${it.port.toString()}")
    }

    install(AutoHeadResponse)
    install(CORS) {
        anyHost()
        method(HttpMethod.Get)
        method(HttpMethod.Head)
        method(HttpMethod.Options)
    }
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    install(ContentNegotiation) {
        json(
            Json{
                prettyPrint = true
            }
        )
    }
    routing {
        cantaloupeReverseProxy(config)
        manifest(nuxeo, config.getConfig("app.iiif"))
        document(nuxeo)
        documents(nuxeo)
        viewer(serverAddressUrl)
    }

}

fun Route.document(nuxeo: Nuxeo) {
    get("/{primaryType}/by/{key}/{value}") {
        try {
            val doc = nuxeo.getDocument(call.parameters)
            if (doc != null) call.respond(doc)
            else {
                call.response.status(HttpStatusCode.NotFound)
                call.respondText("Document of type '${call.parameters["primaryType"]}' with ${call.parameters["key"]} = '${call.parameters["value"]}' not found\n")
            }
        } catch (e: Exception) {
            call.response.status(HttpStatusCode.InternalServerError)
            call.respondText(e.message.toString())
        }
    }
}

fun Route.documents(nuxeo: Nuxeo) {
    get("/pictures/by/folder/{uuid}") {
        call.respond(nuxeo.getDocuments("parentId", call.parameters.getOrFail("uuid"), Nuxeo.PrimaryTypePicture))
    }
}

fun Route.manifest(nuxeo: Nuxeo, config: Config) {
    get ("/manifest/from/folder/by/path/{...}") {
        val path = call.request.uri.replace("/manifest/from/folder/by/path", "")
        val folder = nuxeo.getDocument("path", path, Nuxeo.PrimaryTypeFolder)
        if (folder == null)  {
            call.response.status(HttpStatusCode.NotFound)
            call.respondText("Document of type '${call.parameters["primaryType"]}' with ${call.parameters["key"]} = '${call.parameters["value"]}' not found\n")
        } else {
            val pictures = nuxeo.getDocuments("parentId", folder.uid, Nuxeo.PrimaryTypePicture)
            val manifest = Manifest(config, folder, pictures)
            call.respond(manifest)
        }
    }
    get("/manifest/from/folder/by/{key}/{value}") {
        val folder = nuxeo.getDocument(call.parameters)
        if (folder == null)  {
            call.response.status(HttpStatusCode.NotFound)
            call.respondText("Document of type '${call.parameters["primaryType"]}' with ${call.parameters["key"]} = '${call.parameters["value"]}' not found\n")
        } else {
            val pictures = nuxeo.getDocuments("parentId", folder.uid, Nuxeo.PrimaryTypePicture)
            val manifest = Manifest(config, folder, pictures)
            call.respond(manifest)
        }
    }
}
// Inspired ;-) by https://ktor.kotlincn.net/samples/other/reverse-proxy.html
fun Route.cantaloupeReverseProxy(config: Config) {
    get ("/iiif/{...}") {
        val proxyClient = HttpClient(CIO)
        val response = proxyClient.request<HttpResponse>("${config.getString("app.cantaloupe.server")}${call.request.uri}")
        val proxiedHeaders = response.headers
        val contentType = proxiedHeaders[HttpHeaders.ContentType]
        val contentLength = proxiedHeaders[HttpHeaders.ContentLength]

        // Depending on the ContentType, we process the request one way or another.
        when {
            // In the case of HTML we download the whole content and process it as a string replacing
            // wikipedia links.
            contentType?.startsWith("text/html") == true -> {
                call.respond(
                    TextContent(
                        response.readText(),
                        ContentType.Text.Html.withCharset(Charsets.UTF_8),
                        response.status
                    )
                )
            }
            else -> {
                // In the case of other content, we simply pipe it. We return a [OutgoingContent.WriteChannelContent]
                // propagating the contentLength, the contentType and other headers, and simply we copy
                // the ByteReadChannel from the HTTP client response, to the HTTP server ByteWriteChannel response.
                call.respond(object : OutgoingContent.WriteChannelContent() {
                    override val contentLength: Long? = contentLength?.toLong()
                    override val contentType: ContentType? = contentType?.let { ContentType.parse(it) }
                    override val headers: Headers = Headers.build {
                        appendAll(proxiedHeaders.filter { key, _ ->
                            !key.equals(HttpHeaders.ContentType, ignoreCase = true)
                                    && !key.equals(HttpHeaders.ContentLength, ignoreCase = true)
                                    && !key.equals(HttpHeaders.TransferEncoding, ignoreCase = true)
                        })
                    }
                    override val status: HttpStatusCode = response.status
                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        response.content.copyAndClose(channel)
                    }
                })
            }
        }
    }
}

fun Route.viewer(url: URLBuilder)
{
    data class User(val id: Int, val name: String)
    get("/viewer/folder") {
        val manifest = "${url.buildString()}/manifest/from/folder/by/uid/c7065ef6-ad5a-4235-8f05-1d179bc1e147"
        call.respond(FreeMarkerContent("mirador.ftl", mapOf("manifest" to manifest)))
    }

    get("/viewer/folder/{uuid}") {
        val manifest = "${url.buildString()}/manifest/from/folder/by/uid/${call.parameters["uuid"]}"
        call.respond(FreeMarkerContent("mirador.ftl", mapOf("manifest" to manifest)))
    }

    get ("/viewer/folder/path/{...}") {
        val manifest = "${url.buildString()}${call.request.uri.replace("/viewer/folder/path", "/manifest/from/folder/by/path")}"
        println(manifest)
        call.respond(FreeMarkerContent("mirador.ftl", mapOf("manifest" to manifest)))
    }
}