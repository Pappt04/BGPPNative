package st.misa.bgpp_native.bgpp.presentation.map.provider

import android.content.Context
import st.misa.bgpp_native.BuildConfig
import st.misa.bgpp_native.R

class ThunderforestTransportStyleProvider : StationMapStyleProvider {

    override fun resolveStyle(context: Context): String? {
        val apiKey = BuildConfig.THUNDERFOREST_API_KEY.orEmpty()
        val template = runCatching {
            context.resources.openRawResource(R.raw.map_style_transport)
                .bufferedReader()
                .use { it.readText() }
        }.getOrNull() ?: return null

        return if (apiKey.isBlank()) {
            null
        } else {
            template.replace("%THUNDERFOREST_KEY%", apiKey)
        }
    }
}
