package com.nedaye.rollapp.data

import android.content.Context
import com.nedaye.rollapp.model.FieldDef
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the (hidden) list of entry fields. Default is just the Roll field —
 * this only changes if the person opens the hidden field manager and adds/removes fields.
 */
object Settings {
    private const val PREFS = "app_settings"
    private const val KEY_FIELDS = "field_defs"

    fun defaultFields(): List<FieldDef> = listOf(FieldDef("roll", "New Roll", isRoll = true))

    fun loadFields(ctx: Context): List<FieldDef> {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_FIELDS, null) ?: return defaultFields()
        return try {
            val arr = JSONArray(raw)
            val list = (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                FieldDef(o.getString("id"), o.getString("label"), o.optBoolean("isRoll", false))
            }
            list.ifEmpty { defaultFields() }
        } catch (e: Exception) {
            defaultFields()
        }
    }

    fun saveFields(ctx: Context, fields: List<FieldDef>) {
        val arr = JSONArray()
        fields.forEach {
            val o = JSONObject()
            o.put("id", it.id); o.put("label", it.label); o.put("isRoll", it.isRoll)
            arr.put(o)
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_FIELDS, arr.toString()).apply()
    }

    fun slugify(label: String, existing: List<String>): String {
        var base = label.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        if (base.isEmpty()) base = "field"
        var id = base
        var n = 1
        while (existing.contains(id)) { id = "${base}_$n"; n++ }
        return id
    }
}
