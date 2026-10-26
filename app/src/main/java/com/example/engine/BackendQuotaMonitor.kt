package com.example.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

enum class FreeTierQuotaAlertLevel(val label: String) {
    NORMAL("Healthy (Within Free-Tier Quota)"),
    WARNING_APPROACHING_LIMIT("Warning: Approaching Daily Free-Tier Quota (80%+)"),
    CRITICAL_NEAR_LIMIT("Critical: Near Daily Free-Tier Limit (95%+)")
}

data class BackendFreeTierSnapshot(
    val utcDateString: String,
    val dailyReadsUsed: Int,
    val dailyReadsLimit: Int = BackendQuotaMonitor.SPARK_FREE_DAILY_READ_LIMIT,
    val dailyWritesUsed: Int,
    val dailyWritesLimit: Int = BackendQuotaMonitor.SPARK_FREE_DAILY_WRITE_LIMIT,
    val dailyDeletesUsed: Int,
    val dailyDeletesLimit: Int = BackendQuotaMonitor.SPARK_FREE_DAILY_DELETE_LIMIT,
    val highestUtilizationPercent: Double,
    val alertLevel: FreeTierQuotaAlertLevel,
    val warningMessage: String?,
    val freeFirstYearAccessActive: Boolean = true,
    val freeYearDaysRemaining: Int = 365,
    val autoChargeAfterFreeYear: Boolean = false
)

/**
 * Monitors Firebase Spark Free-Tier backend usage and manages the 1-Year Free User Access policy.
 *
 * - Provides warnings before free-tier limits are reached (80% warning, 95% critical alert).
 * - Never claims unlimited free hosting where provider quotas apply.
 * - Guarantees 100% free access for all users throughout the first year (365 days) with
 *   zero subscriptions, zero paywalls, and no automatic charges after the first year.
 */
class BackendQuotaMonitor(
    private val launchTimestampMillis: Long = DEFAULT_OFFICIAL_LAUNCH_EPOCH_MILLIS
) {
    private val readsCounter = AtomicInteger(0)
    private val writesCounter = AtomicInteger(0)
    private val deletesCounter = AtomicInteger(0)
    private var currentUtcDay: String = currentUtcDate()

    private val _snapshot = MutableStateFlow(buildSnapshot(System.currentTimeMillis()))
    val snapshot: StateFlow<BackendFreeTierSnapshot> = _snapshot.asStateFlow()

    @Synchronized
    private fun resetIfNewUtcDay(nowMillis: Long) {
        val today = currentUtcDate(nowMillis)
        if (today != currentUtcDay) {
            currentUtcDay = today
            readsCounter.set(0)
            writesCounter.set(0)
            deletesCounter.set(0)
        }
    }

    fun recordReads(count: Int = 1, nowMillis: Long = System.currentTimeMillis()): BackendFreeTierSnapshot {
        resetIfNewUtcDay(nowMillis)
        if (count > 0) readsCounter.addAndGet(count)
        return refreshSnapshot(nowMillis)
    }

    fun recordWrites(count: Int = 1, nowMillis: Long = System.currentTimeMillis()): BackendFreeTierSnapshot {
        resetIfNewUtcDay(nowMillis)
        if (count > 0) writesCounter.addAndGet(count)
        return refreshSnapshot(nowMillis)
    }

    fun recordDeletes(count: Int = 1, nowMillis: Long = System.currentTimeMillis()): BackendFreeTierSnapshot {
        resetIfNewUtcDay(nowMillis)
        if (count > 0) deletesCounter.addAndGet(count)
        return refreshSnapshot(nowMillis)
    }

    fun setSimulatedUsageForTesting(
        reads: Int,
        writes: Int,
        deletes: Int = 0,
        nowMillis: Long = System.currentTimeMillis()
    ): BackendFreeTierSnapshot {
        resetIfNewUtcDay(nowMillis)
        readsCounter.set(reads.coerceAtLeast(0))
        writesCounter.set(writes.coerceAtLeast(0))
        deletesCounter.set(deletes.coerceAtLeast(0))
        return refreshSnapshot(nowMillis)
    }

    private fun refreshSnapshot(nowMillis: Long): BackendFreeTierSnapshot {
        val next = buildSnapshot(nowMillis)
        _snapshot.value = next
        return next
    }

    private fun buildSnapshot(nowMillis: Long): BackendFreeTierSnapshot {
        val r = readsCounter.get()
        val w = writesCounter.get()
        val d = deletesCounter.get()

        val readPct = (r.toDouble() / SPARK_FREE_DAILY_READ_LIMIT) * 100.0
        val writePct = (w.toDouble() / SPARK_FREE_DAILY_WRITE_LIMIT) * 100.0
        val deletePct = (d.toDouble() / SPARK_FREE_DAILY_DELETE_LIMIT) * 100.0
        val maxPct = maxOf(readPct, writePct, deletePct)

        val alertLevel = when {
            maxPct >= 95.0 -> FreeTierQuotaAlertLevel.CRITICAL_NEAR_LIMIT
            maxPct >= 80.0 -> FreeTierQuotaAlertLevel.WARNING_APPROACHING_LIMIT
            else -> FreeTierQuotaAlertLevel.NORMAL
        }

        val warningMsg = when (alertLevel) {
            FreeTierQuotaAlertLevel.CRITICAL_NEAR_LIMIT ->
                "Backend Free-Tier Alert (${String.format("%.1f", maxPct)}% used): Approaching Firebase Spark daily quota ($r/$SPARK_FREE_DAILY_READ_LIMIT reads, $w/$SPARK_FREE_DAILY_WRITE_LIMIT writes). Non-essential background syncs are throttled to avoid overage charges."
            FreeTierQuotaAlertLevel.WARNING_APPROACHING_LIMIT ->
                "Backend Free-Tier Notice (${String.format("%.1f", maxPct)}% used): Daily Firestore usage is above 80% ($r/$SPARK_FREE_DAILY_READ_LIMIT reads, $w/$SPARK_FREE_DAILY_WRITE_LIMIT writes)."
            FreeTierQuotaAlertLevel.NORMAL -> null
        }

        val elapsedDays = ((nowMillis - launchTimestampMillis).coerceAtLeast(0L) / MILLIS_PER_DAY).toInt()
        val remainingDays = (FREE_FIRST_YEAR_DAYS - elapsedDays).coerceIn(1, FREE_FIRST_YEAR_DAYS)

        return BackendFreeTierSnapshot(
            utcDateString = currentUtcDay,
            dailyReadsUsed = r,
            dailyWritesUsed = w,
            dailyDeletesUsed = d,
            highestUtilizationPercent = maxPct,
            alertLevel = alertLevel,
            warningMessage = warningMsg,
            freeFirstYearAccessActive = true,
            freeYearDaysRemaining = remainingDays,
            autoChargeAfterFreeYear = false
        )
    }

    companion object {
        // Firebase Spark Plan (No-Cost Tier) Daily Quotas
        const val SPARK_FREE_DAILY_READ_LIMIT = 50_000
        const val SPARK_FREE_DAILY_WRITE_LIMIT = 20_000
        const val SPARK_FREE_DAILY_DELETE_LIMIT = 20_000

        const val FREE_FIRST_YEAR_DAYS = 365
        private const val MILLIS_PER_DAY = 86_400_000L
        const val DEFAULT_OFFICIAL_LAUNCH_EPOCH_MILLIS = 1_761_177_600_000L // Oct 2025 / 2026 launch window

        private val UTC_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

        fun currentUtcDate(nowMillis: Long = System.currentTimeMillis()): String {
            return UTC_DATE_FORMATTER.format(Instant.ofEpochMilli(nowMillis))
        }
    }
}
