package nu.lindeman.iiif.nuxeo

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class NXQL (
    @Transient
    val query: String = ""
) {
    val params = Params(query)
    val context = Context()

    @Serializable
    inner class Params(val query: String)
    @Serializable
    inner class Context()

}
