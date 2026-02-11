package com.faulk.appkiller.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    val lastUsedTimestamp: Long,
    val type: AppType,
    // Nothing is selected by default. The repository may set this for user apps.
    var isSelected: Boolean = false
)