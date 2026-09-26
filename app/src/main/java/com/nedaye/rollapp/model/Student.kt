package com.nedaye.rollapp.model

data class Student(
    val id: Int,
    val name: String,
    val father: String,
    val mother: String,
    var roll: String = "",
    val extra: MutableMap<String, String> = mutableMapOf(),
    val originalRow: List<String> = emptyList()
)

fun Student.isBlank() = roll.isBlank()
fun Student.isInvalid() = roll.isNotBlank() && !roll.all { it.isDigit() }
fun Student.isDup(all: List<Student>) =
    roll.isNotBlank() && roll.all { it.isDigit() } &&
        all.any { it.id != id && it.roll == roll }

fun Student.valueFor(fieldId: String): String = if (fieldId == "roll") roll else extra[fieldId] ?: ""

/** Deep copy — needed for undo snapshots since `extra` is a mutable map. */
fun Student.deepCopy(): Student = copy(extra = extra.toMutableMap())
