package com.example.engine

import androidx.room.withTransaction
import com.example.data.local.DailyRewardEntity
import com.example.data.local.LifeAssetsDatabase
import com.example.data.model.ConnectionStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class DailyRewardStatus(
    val kathmanduDateString: String,
    val isAvailableToday: Boolean,
    val currentStreak: Int,
    val nextRewardAmount: Double,
    val countdownFormatted: String,
    val statusLabel: String,
    val isOnlineReady: Boolean = true,
    val lastClaimedAtMillis: Long? = null,
    val nextEligibleAtMillis: Long? = null,
    val remainingCooldownMillis: Long = 0L
) {
    val isEligibleNow: Boolean
        get() = isAvailableToday && isOnlineReady && remainingCooldownMillis <= 0L
}

class DailyRewardService(
    private val db: LifeAssetsDatabase,
    private val backendService: MarketDataBackendService? = null
) {

    private val claimMutex = Mutex()

    companion object {
        val KATHMANDU_ZONE: ZoneId = ZoneId.of("Asia/Kathmandu")
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        const val REWARD_COOLDOWN_MILLIS: Long = 24L * 60L * 60L * 1000L // Exact 24 hours (86,400,000 ms)
        const val STREAK_GRACE_WINDOW_MILLIS: Long = 48L * 60L * 60L * 1000L // 48 hours before streak resets

        fun getKathmanduDateString(epochMillis: Long = System.currentTimeMillis()): String {
            val zdt = Instant.ofEpochMilli(epochMillis).atZone(KATHMANDU_ZONE)
            return zdt.toLocalDate().format(DATE_FORMATTER)
        }

        fun calculateStreak(
            lastClaimDateString: String?,
            todayDateString: String,
            previousStreak: Int
        ): Int {
            if (lastClaimDateString.isNullOrBlank()) return 1
            val cleanLast = lastClaimDateString.substringBefore("_")
            val cleanToday = todayDateString.substringBefore("_")
            val lastDate = LocalDate.parse(cleanLast, DATE_FORMATTER)
            val todayDate = LocalDate.parse(cleanToday, DATE_FORMATTER)
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastDate, todayDate)
            return when {
                daysBetween <= 1L -> (previousStreak + 1).coerceAtLeast(1)
                else -> 1
            }
        }

        fun calculateStreakFromTimestamps(
            lastClaimedAtMillis: Long?,
            nowServerMillis: Long,
            previousStreak: Int
        ): Int {
            if (lastClaimedAtMillis == null || lastClaimedAtMillis <= 0L) return 1
            val elapsed = nowServerMillis - lastClaimedAtMillis
            return when {
                elapsed < REWARD_COOLDOWN_MILLIS -> previousStreak.coerceAtLeast(1)
                elapsed <= STREAK_GRACE_WINDOW_MILLIS -> (previousStreak + 1).coerceAtLeast(1)
                else -> 1
            }
        }

        fun rewardForStreakDay(streakDay: Int): Double {
            return when (streakDay.coerceAtLeast(1)) {
                1 -> 50_000.0
                2 -> 75_000.0
                3 -> 125_000.0
                4 -> 200_000.0
                5 -> 350_000.0
                6 -> 500_000.0
                else -> 1_000_000.0
            }
        }

        fun formatCooldownDuration(remainingMillis: Long): String {
            if (remainingMillis <= 0L) return "READY NOW"
            val totalSeconds = (remainingMillis + 999L) / 1000L
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
        }

        fun getCountdownToKathmanduMidnight(epochMillis: Long = System.currentTimeMillis()): String {
            val nowKtm: ZonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(KATHMANDU_ZONE)
            val nextMidnight: ZonedDateTime = nowKtm.toLocalDate().plusDays(1).atStartOfDay(KATHMANDU_ZONE)
            val duration = Duration.between(nowKtm, nextMidnight)
            val totalSeconds = duration.seconds.coerceAtLeast(0L)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
        }
    }

    private fun isOnlineReady(): Boolean {
        val svc = backendService ?: return true
        return svc.connectionStatus.value != ConnectionStatus.OFFLINE &&
            svc.connectionStatus.value != ConnectionStatus.CONNECTING &&
            svc.backendHealth.value != BackendHealthState.BACKEND_DOWN_503
    }

    suspend fun getRewardStatus(
        userId: String,
        serverTimeMillis: Long = backendService?.getAuthoritativeServerTimeMillis() ?: System.currentTimeMillis()
    ): DailyRewardStatus = getStatus(userId, serverTimeMillis)

    suspend fun getStatus(
        userId: String,
        serverTimeMillis: Long = backendService?.getAuthoritativeServerTimeMillis() ?: System.currentTimeMillis()
    ): DailyRewardStatus {
        val online = isOnlineReady()
        val todayKtm = getKathmanduDateString(serverTimeMillis)
        val user = db.userDao().getUserById(userId)
        val latestClaim = db.dailyRewardDao().getLatestClaimForUser(userId)
        val lastClaimMillis = latestClaim?.claimedAtMillis
        val nextEligibleMillis = lastClaimMillis?.plus(REWARD_COOLDOWN_MILLIS)
        val remainingMillis = if (nextEligibleMillis != null) {
            (nextEligibleMillis - serverTimeMillis).coerceAtLeast(0L)
        } else {
            0L
        }
        val isCooldownActive = remainingMillis > 0L
        val nextStreak = if (isCooldownActive) {
            user?.streakDays ?: latestClaim?.streakDay ?: 1
        } else {
            calculateStreakFromTimestamps(lastClaimMillis, serverTimeMillis, user?.streakDays ?: 0)
        }
        val rewardAmount = rewardForStreakDay(nextStreak)
        val countdown = if (isCooldownActive) {
            formatCooldownDuration(remainingMillis)
        } else {
            "READY NOW"
        }

        return DailyRewardStatus(
            kathmanduDateString = todayKtm,
            isAvailableToday = !isCooldownActive && online,
            currentStreak = user?.streakDays ?: 0,
            nextRewardAmount = rewardAmount,
            countdownFormatted = countdown,
            statusLabel = when {
                !online -> "Offline • Trusted Server Time Required"
                isCooldownActive -> "24h Cooldown Active • Next claim in $countdown"
                else -> "Available Now (24h Server-Verified)"
            },
            isOnlineReady = online,
            lastClaimedAtMillis = lastClaimMillis,
            nextEligibleAtMillis = nextEligibleMillis,
            remainingCooldownMillis = remainingMillis
        )
    }

    suspend fun claimDailyReward(
        userId: String,
        authoritativeServerTimeMillis: Long = backendService?.getAuthoritativeServerTimeMillis() ?: System.currentTimeMillis()
    ): Result<DailyRewardEntity> = claimMutex.withLock {
        if (!isOnlineReady()) {
            return@withLock Result.failure(
                IllegalStateException("Internet connection required: Offline daily reward claiming is disabled.")
            )
        }

        return@withLock try {
            val claim = db.withTransaction {
                val user = db.userDao().getUserById(userId)
                    ?: throw IllegalStateException("Authenticated user not found.")

                val latestClaim = db.dailyRewardDao().getLatestClaimForUser(userId)
                val lastClaimMillis = latestClaim?.claimedAtMillis

                // Validate trusted server timestamp against rollback and future clock manipulation
                if (backendService != null) {
                    val timeValidation = backendService.validateTrustedServerTimestamp(
                        proposedTimeMillis = authoritativeServerTimeMillis,
                        lastRecordedEventMillis = lastClaimMillis
                    )
                    if (timeValidation.isFailure) {
                        throw timeValidation.exceptionOrNull()!!
                    }
                } else if (lastClaimMillis != null && authoritativeServerTimeMillis < lastClaimMillis) {
                    throw IllegalStateException(
                        "Clock manipulation detected: Device clock was rolled back before last reward claim."
                    )
                }

                // Enforce exact 24-hour (86,400,000 ms) eligibility after last successful claim
                if (lastClaimMillis != null) {
                    val nextEligibleMillis = lastClaimMillis + REWARD_COOLDOWN_MILLIS
                    val remaining = nextEligibleMillis - authoritativeServerTimeMillis
                    if (remaining > 0L) {
                        val formatted = formatCooldownDuration(remaining)
                        throw IllegalStateException(
                            "24-hour cooldown active: Next daily reward eligible in $formatted (exactly 24 hours after last claim)."
                        )
                    }
                }

                val todayKtm = getKathmanduDateString(authoritativeServerTimeMillis)
                val existingSameDateKey = db.dailyRewardDao().getClaimForDate(userId, todayKtm)
                val claimKeyDate = if (existingSameDateKey == null) {
                    todayKtm
                } else {
                    "${todayKtm}_$authoritativeServerTimeMillis"
                }

                val newStreak = calculateStreakFromTimestamps(
                    lastClaimedAtMillis = lastClaimMillis,
                    nowServerMillis = authoritativeServerTimeMillis,
                    previousStreak = user.streakDays
                )
                val rewardAmount = rewardForStreakDay(newStreak)

                val record = DailyRewardEntity(
                    userId = userId,
                    claimDateKathmandu = claimKeyDate,
                    streakDay = newStreak,
                    rewardAmount = rewardAmount,
                    claimedAtMillis = authoritativeServerTimeMillis
                )
                db.dailyRewardDao().insertClaim(record)
                db.userDao().updateUser(
                    user.copy(
                        cashBalance = user.cashBalance + rewardAmount,
                        streakDays = newStreak,
                        lastClaimDateKathmandu = todayKtm
                    )
                )
                record
            }
            Result.success(claim)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
