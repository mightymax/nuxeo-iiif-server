package nu.lindeman.iiif

import com.typesafe.config.Config
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.util.*
import io.vertx.core.net.impl.TrustAllTrustManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nu.lindeman.iiif.nuxeo.*
import kotlin.Exception


class Nuxeo(val config: Config) {

    val nxqlEndpoint = URLBuilder(config.getString("url")).path(config.getString("path.nxql"))
    val blobEndpoint = URLBuilder(config.getString("url")).path(config.getString("path.blob"))
    companion object {
        val ContentTypeNXRequest: ContentType = ContentType("application", "json+nxrequest")
        const val PrimaryTypePicture: String = "Picture"
        const val PrimaryTypeFolder: String = "Folder"
    }

    private val client: HttpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(config.getString("username"), config.getString("password"))
                }
            }
        }
        engine {
            config.tryGetString("proxy").let{
                proxy = ProxyBuilder.http(it.toString())
            }
            https {
                trustManager = TrustAllTrustManager.INSTANCE
            }
        }
    }

    fun getClient(): HttpClient {
        return client
    }

    private suspend fun nxql(query: NXQL): Documents {
        return client.post<Documents>(nxqlEndpoint.buildString()) {
            headers{
                append(HttpHeaders.ContentType, ContentTypeNXRequest.toString())
                append("properties", "*")
            }
            body = Json.encodeToString(query)
        }
    }

    suspend fun getDocuments(parameters: Parameters): Documents {
        val key = parameters.getOrFail("key")
        val value = parameters.getOrFail("value")
        val primaryType = parameters["primaryType"]
        return getDocuments(key, value, primaryType)
    }

    suspend fun getDocuments(key: String, value: String, primaryTypeOrNull: String? = null): Documents {
        val primaryType: String? = primaryTypeOrNull?.replaceFirstChar{ it.titlecase() }
        var fieldname: String? = when (key) {
            "uid", "uuid" -> "ecm:uuid"
            "name" -> "dc:title"
            "parentId" -> "ecm:parentId"
            "path" -> "ecm:path"
            else -> null
        }
        if (primaryType == PrimaryTypePicture && fieldname == null)
            fieldname = when (key) {
                "filename" -> "file:content/name"
                else -> null
            }
        if (null == fieldname) throw Exception("Can not map key '$key' to a Nuxeo property for primaryType '${primaryType?.replaceFirstChar{ it.titlecase() }}'")

        var queryString = "SELECT * FROM Document WHERE ecm:isTrashed = 0 AND ecm:isProxy = 0 AND $fieldname='$value'"
        if (primaryType == PrimaryTypePicture || primaryType == PrimaryTypeFolder)
            queryString += " AND ecm:primaryType='$primaryType'"

        return nxql(NXQL(queryString))
    }

    suspend fun getDocument(parameters: Parameters): Document? {
        val result = getDocuments(parameters)
        if (result.resultsCount > 0 ) {
            return result.entries[0]
        }
        return null
    }

    suspend fun getDocument(key: String, value: String, primaryType: String? = null): Document? {
        val result = getDocuments(key, value, primaryType)
        if (result.resultsCount > 0 ) {
            return result.entries[0]
        }
        return null
    }
}