package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String = "" // Added custom label for organization
)

@Entity(tableName = "history")
data class History(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "extensions")
data class Extension(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val iconUrl: String,
    val isEnabled: Boolean = true,
    val permissions: String = "", // Comma-separated list
    val sourceUrl: String = "",
    val isVerified: Boolean = false
)

