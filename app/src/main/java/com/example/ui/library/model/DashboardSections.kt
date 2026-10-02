package com.example.ui.library.model

/**
 * Reorderable / hideable sections of the Library dashboard.
 * The header, search pill and Play All / Shuffle actions are fixed and not part of this list.
 */
enum class DashboardSection(val id: String, val label: String) {
    RESUME("resume", "Continue listening"),
    BROWSE("browse", "Browse"),
    RECENTLY_PLAYED("recent", "Recently played"),
    MADE_FOR_YOU("mixes", "Made for you"),
    LISTENING_STATS("stats", "Your listening"),
    RECENTLY_ADDED("added", "Recently added");

    companion object {
        fun fromId(id: String): DashboardSection? = entries.firstOrNull { it.id == id }
    }
}

data class DashboardSectionPref(val section: DashboardSection, val visible: Boolean)

val DefaultDashboardSections: List<DashboardSectionPref> =
    DashboardSection.entries.map { DashboardSectionPref(it, true) }

/**
 * Parses the persisted "id:1,id:0,..." string. Unknown ids are dropped, duplicates ignored and
 * sections added in a later app version are appended (visible) so users never lose new content.
 */
fun parseDashboardSections(raw: String?): List<DashboardSectionPref> {
    if (raw.isNullOrBlank()) return DefaultDashboardSections
    val parsed = raw.split(',').mapNotNull { token ->
        val parts = token.trim().split(':')
        val section = DashboardSection.fromId(parts.getOrNull(0).orEmpty()) ?: return@mapNotNull null
        DashboardSectionPref(section, parts.getOrNull(1) != "0")
    }.distinctBy { it.section }
    val missing = DashboardSection.entries
        .filter { section -> parsed.none { it.section == section } }
        .map { DashboardSectionPref(it, true) }
    return parsed + missing
}

fun List<DashboardSectionPref>.encodeDashboardSections(): String =
    joinToString(",") { "${it.section.id}:${if (it.visible) 1 else 0}" }
