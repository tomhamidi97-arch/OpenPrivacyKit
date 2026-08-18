package com.openprivacykit.app.ui

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

/**
 * Minimal, dependency-free configuration UI.
 * Lists installed apps; tapping one opens a per-app spoof editor.
 * All values are stored in PREFS_FILE as "<pkg>|<field>" keys.
 */
class MainActivity : Activity() {

    companion object {
        const val PREFS_FILE = "openprivacykit_prefs"
        private val EDITABLE_FIELDS = listOf(
            "android_id", "build_model", "build_manufacturer", "build_brand",
            "build_device", "build_fingerprint", "build_product", "ad_id", "mac"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showAppList()
    }

    private fun showAppList() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = "OpenPrivacyKit\nSelect an app to configure spoofed identifiers."
            textSize = 16f
            setPadding(0, 0, 0, 32)
        })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        for (app in apps) {
            val row = TextView(this).apply {
                text = "${app.loadLabel(pm)}  (${app.packageName})"
                textSize = 15f
                setPadding(24, 28, 24, 28)
                setOnClickListener { showEditor(app.packageName) }
            }
            list.addView(row)
        }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
    }

    private fun showEditor(pkg: String) {
        val sp = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        root.addView(TextView(this).apply {
            text = pkg; textSize = 18f; setPadding(0, 0, 0, 24)
        })

        val enableBox = CheckBox(this).apply { text = "Enable spoofing for this app" }
        enableBox.isChecked = sp.getBoolean("$pkg|enable", false)
        root.addView(enableBox)

        val inputs = mutableMapOf<String, EditText>()
        for (field in EDITABLE_FIELDS) {
            root.addView(TextView(this).apply { text = field; setPadding(0, 24, 0, 8) })
            val et = EditText(this).apply {
                hint = if (field == "android_id" || field == "ad_id" || field == "mac")
                    "! = auto random" else "leave empty = real value"
            }
            et.setText(sp.getString("$pkg|$field", ""))
            inputs[field] = et
            root.addView(et)
        }

        root.addView(Button(this).apply {
            text = "Save"
            setOnClickListener {
                sp.edit().apply {
                    putBoolean("$pkg|enable", enableBox.isChecked)
                    for ((f, et) in inputs) putString("$pkg|$f", et.text.toString())
                }.apply()
                Toast.makeText(this@MainActivity, "Saved. Force-stop the target app to apply.", Toast.LENGTH_LONG).show()
                finish()
            }
        })

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
    }
}
