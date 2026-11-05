package com.nishant.northmusic.data.model

enum class DownloadFolderStructure(val label: String, val subtitle: String) {
    CATEGORY_ARTIST("Category / Artist", "Organizes files into Music/<Category>/<Artist>/"),
    ARTIST_ONLY("Artist only", "Organizes files into Music/<Artist>/"),
    FLAT("Flat (Music folder)", "Saves files directly into Music/");

    val displayName: String get() = label
}
