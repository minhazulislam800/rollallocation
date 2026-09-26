package com.nedaye.rollapp.data

import android.content.Context
import com.nedaye.rollapp.model.Student
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SessionStore {
    private fun sessionsDir(ctx: Context): File {
        val d = File(ctx.filesDir, "sessions")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun fingerprint(headers: List<String>, students: List<Student>): String {
        val sample = students.take(8).joinToString("~") { "${it.name}|${it.father}|${it.mother}" }
        val raw = headers.joinToString(",") + "::" + students.size + "::" + sample
        var h = 5381L
        for (c in raw) { h = ((h shl 5) + h) + c.code; h = h and 0xFFFFFFFFL }
        return h.toString(16)
    }

    fun save(ctx: Context, fingerprint: String, fileName: String, headers: List<String>, students: List<Student>) {
        val obj = JSONObject()
        obj.put("fileName", fileName)
        obj.put("headers", JSONArray(headers))
        val arr = JSONArray()
        students.forEach {
            val o = JSONObject()
            o.put("id", it.id); o.put("name", it.name); o.put("father", it.father)
            o.put("mother", it.mother); o.put("roll", it.roll)
            o.put("originalRow", JSONArray(it.originalRow))
            val extraObj = JSONObject()
            it.extra.forEach { (k, v) -> extraObj.put(k, v) }
            o.put("extra", extraObj)
            arr.put(o)
        }
        obj.put("students", arr)
        File(sessionsDir(ctx), "$fingerprint.json").writeText(obj.toString())
        File(ctx.filesDir, "current.txt").writeText(fingerprint)
    }

    fun loadCurrent(ctx: Context): Triple<String, List<String>, List<Student>>? {
        val cur = File(ctx.filesDir, "current.txt")
        if (!cur.exists()) return null
        val fp = cur.readText().trim()
        return load(ctx, fp)
    }

    fun load(ctx: Context, fingerprint: String): Triple<String, List<String>, List<Student>>? {
        val f = File(sessionsDir(ctx), "$fingerprint.json")
        if (!f.exists()) return null
        val obj = JSONObject(f.readText())
        val fileName = obj.optString("fileName", "")
        val headers = mutableListOf<String>()
        val hArr = obj.optJSONArray("headers") ?: JSONArray()
        for (i in 0 until hArr.length()) headers.add(hArr.getString(i))
        val students = mutableListOf<Student>()
        val sArr = obj.optJSONArray("students") ?: JSONArray()
        for (i in 0 until sArr.length()) {
            val o = sArr.getJSONObject(i)
            val orArr = o.optJSONArray("originalRow") ?: JSONArray()
            val orList = (0 until orArr.length()).map { orArr.getString(it) }
            val extraObj = o.optJSONObject("extra")
            val extraMap: MutableMap<String, String> = mutableMapOf()
            if (extraObj != null) {
                val keys = extraObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    extraMap[k] = extraObj.getString(k)
                }
            }
            students.add(
                Student(
                    id = o.getInt("id"),
                    name = o.getString("name"),
                    father = o.getString("father"),
                    mother = o.getString("mother"),
                    roll = o.optString("roll", ""),
                    extra = extraMap,
                    originalRow = orList
                )
            )
        }
        return Triple(fileName, headers, students)
    }

    fun clearCurrent(ctx: Context) {
        File(ctx.filesDir, "current.txt").delete()
    }
}
