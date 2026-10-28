package dev.bxane.rynx.android

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource

/** Attaches ephemeral provider headers to only the URL that owns them. */
@UnstableApi
internal class ProviderHeaderResolver(
    private val headersFor: (String) -> Map<String, String>,
) : ResolvingDataSource.Resolver {
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val headers = headersFor(dataSpec.uri.toString())
        return if (headers.isEmpty()) {
            dataSpec
        } else {
            dataSpec.buildUpon().setHttpRequestHeaders(headers).build()
        }
    }
}
