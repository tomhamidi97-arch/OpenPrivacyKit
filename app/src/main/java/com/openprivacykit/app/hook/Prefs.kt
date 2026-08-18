package com.openprivacykit.app.hook

import android.content.Context
import de.robv.android.xposed.XSharedPreferences

/**
 * Per-app spoof configuration. Settings are written by the UI into the
 * module's own shared preferences and read inside target app processes
 * via XSharedPreferences (world-readable).
 *
 * Key layout:
 *   "<packageName>|android_id"  -> spoofed value ("!" = randomize per boot)
 *   "<packageName>|enable"      -> "true"/"false"
 *   "global|<field>"           -> default applied when per-app value absent
 */
object Prefs {
    const val PREFS_FILE = "openprivacykit_prefs"

    fun load(packageName: String): Map<String, String> {
        return try {
            val xsp = XSharedPreferences("com.openprivacykit.app", PREFS_FILE)
            xsp.makeWorldReadable()
            val all = xsp.all
            val out = mutableMapOf<String, String>()
            for ((k, v) in all) {
                val str = v?.toString() ?: continue
                if (k.startsWith("$packageName|") || k.startsWith("global|")) {
                    // per-app value overrides global default
                    val field = k.substringAfter('|')
                    if (k.startsWith("$packageName|")) out[field] = str else out.putIfAbsent(field, str)
                }
            }
            out
        } catch (t: Throwable) {
            emptyMap()
        }
    }

    fun enabled(prefs: Map<String, String>): Boolean =
        prefs["enable"]?.toBoolean() ?: false
}
