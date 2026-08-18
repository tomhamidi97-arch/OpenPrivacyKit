package com.openprivacykit.app.hook

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Xposed entry point. Hooks run inside every scoped app process.
 */
class EntryHook : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "OpenPrivacyKit"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Never hook ourselves or core system components.
        if (lpparam.packageName == "com.openprivacykit.app" ||
            lpparam.packageName == "android"
        ) return

        val prefs = Prefs.load(lpparam.packageName)
        if (!Prefs.enabled(prefs)) return

        log("active for ${lpparam.packageName}")

        HookAndroidId(lpparam, prefs).apply()
        HookBuildFields(lpparam, prefs).apply()
        HookAdvertisingId(lpparam, prefs).apply()
        HookNetInfo(lpparam, prefs).apply()
    }

    private fun log(msg: String) {
        XposedBridge.log("[$TAG] $msg")
    }
}

/** Base class: each spoof target is one small, independently auditable hook. */
abstract class BaseHook(
    protected val lpparam: XC_LoadPackage.LoadPackageParam,
    protected val prefs: Map<String, String>
) {
    abstract fun apply()
}

/** Settings.Secure.ANDROID_ID */
class HookAndroidId(lpparam: XC_LoadPackage.LoadPackageParam, prefs: Map<String, String>) :
    BaseHook(lpparam, prefs) {

    override fun apply() {
        val value = prefs["android_id"]
        if (value.isNullOrEmpty()) return
        val spoofed = if (value == "!") SpoofValues.androidId(lpparam.packageName) else value

        XposedHelpers.findAndHookMethod(
            "android.provider.Settings\$Secure", lpparam.classLoader,
            "getStringForUser", android.content.ContentResolver::class.java,
            String::class.java, Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.args[1] == "android_id") param.result = spoofed
                }
            })
        XposedBridge.log("[OpenPrivacyKit] android_id spoofed for ${lpparam.packageName}")
    }
}

/** Build.MODEL / Build.MANUFACTURER / Build.BRAND / Build.FINGERPRINT etc. */
class HookBuildFields(lpparam: XC_LoadPackage.LoadPackageParam, prefs: Map<String, String>) :
    BaseHook(lpparam, prefs) {

    private val fields = mapOf(
        "build_model" to "MODEL",
        "build_manufacturer" to "MANUFACTURER",
        "build_brand" to "BRAND",
        "build_device" to "DEVICE",
        "build_fingerprint" to "FINGERPRINT",
        "build_product" to "PRODUCT",
    )

    override fun apply() {
        // Static final fields — set via reflection on the Build class.
        for ((key, field) in fields) {
            val value = prefs[key] ?: continue
            try {
                val buildClass = XposedHelpers.findClass("android.os.Build", lpparam.classLoader)
                val f = buildClass.getDeclaredField(field)
                f.isAccessible = true
                f.set(null, value)
            } catch (t: Throwable) {
                XposedBridge.log("[OpenPrivacyKit] failed Build.$field: ${t.message}")
            }
        }
    }
}

/**
 * Google Play Services Advertising ID.
 * Hooks both the old AdvertisingIdClient info getter and reflection-proof
 * getAdvertisingIdInfo result class. Blocking is chosen by pref value
 * "" (no hook) / "!" (zero ad id).
 */
class HookAdvertisingId(lpparam: XC_LoadPackage.LoadPackageParam, prefs: Map<String, String>) :
    BaseHook(lpparam, prefs) {

    override fun apply() {
        val mode = prefs["ad_id"] ?: return
        if (mode != "!") return

        val cl = lpparam.classLoader
        // a.a.a.xyz pattern in old gms; use class-name lookup defensively
        for (name in listOf(
            "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info"
        )) {
            try {
                val infoClass = XposedHelpers.findClass(name, cl)
                XposedHelpers.findAndHookMethod(
                    infoClass, "getId",
                    XC_MethodReplacement.returnConstant("00000000-0000-0000-0000-000000000000")
                )
            } catch (_: Throwable) { /* gms not present in this app */ }
        }
    }
}

/** NetworkInterface hardware address + WifiInfo SSID/BSSID. */
class HookNetInfo(lpparam: XC_LoadPackage.LoadPackageParam, prefs: Map<String, String>) :
    BaseHook(lpparam, prefs) {

    override fun apply() {
        if (prefs["mac"] == "!") {
            try {
                XposedHelpers.findAndHookMethod(
                    "java.net.NetworkInterface", lpparam.classLoader,
                    "getHardwareAddress",
                    XC_MethodReplacement.returnConstant(
                        SpoofValues.macAddress().split(":")
                            .map { it.toInt(16).toByte() }.toByteArray()
                    )
                )
            } catch (t: Throwable) {
                XposedBridge.log("[OpenPrivacyKit] mac hook failed: ${t.message}")
            }
        }
    }
}
