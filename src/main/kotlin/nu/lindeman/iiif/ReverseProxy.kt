package nu.lindeman.iiif

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import io.ktor.utils.io.*

// Inspired ;-) by https://ktor.kotlincn.net/samples/other/reverse-proxy.html
class ReverseProxy(call: ApplicationCall, urlString: String) {
    var call: ApplicationCall
    var urlString: String

    var allowHtml: Boolean = true

    init {
        this.call = call
        this.urlString = urlString
    }

    fun noHtml(): ReverseProxy {
        allowHtml = false
        return this
    }

    suspend fun request(client: HttpClient? = null) {
        val proxyClient: HttpClient = client ?: HttpClient(CIO)

        val response = try {proxyClient.request<HttpResponse>(urlString)} catch (e: ClientRequestException) {
            call.respond(e.response.status, "HTTP error ${e.response.status} " +
                    "for client request `${e.response.call.request.url}`.")
            return
        }
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
                if (allowHtml) {
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
}