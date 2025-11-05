package st.misa.bgpp_native.bgpp.presentation.map.provider

import android.content.Context

interface StationMapStyleProvider {
    /**
     * Returns a fully qualified style JSON (or URI) understood by MapLibre.
     * Returning null signals that the default vector style should be used.
     */
    fun resolveStyle(context: Context): String?
}
