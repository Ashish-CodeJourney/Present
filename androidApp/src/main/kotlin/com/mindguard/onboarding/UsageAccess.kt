package com.mindguard.onboarding

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
private const val MILLIS_PER_MINUTE = 60L * 1000

fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun usageAccessSettingsIntent(): Intent =
    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun averageDailyMinutes(context: Context, apps: Set<TrackedApp>, days: Int = 7): Map<TrackedApp, Int> {
    val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        ?: return emptyMap()
    val safeDays = days.coerceAtLeast(1)
    val end = System.currentTimeMillis()
    val start = end - safeDays * MILLIS_PER_DAY
    val foregroundByPackage = runCatching { usageStats.queryAndAggregateUsageStats(start, end) }
        .getOrNull()
        ?.mapValues { it.value.totalTimeInForeground }
        ?: return emptyMap()
    return apps.associateWith { app ->
        val totalMillis = app.packageNames.sumOf { foregroundByPackage[it] ?: 0L }
        (totalMillis / MILLIS_PER_MINUTE / safeDays).toInt()
    }
}
