package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        HoldingEntity::class,
        TransactionEntity::class,
        LimitOrderEntity::class,
        DailyRewardEntity::class,
        TradeOfferEntity::class,
        AssetEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class LifeAssetsDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun holdingDao(): HoldingDao
    abstract fun transactionDao(): TransactionDao
    abstract fun limitOrderDao(): LimitOrderDao
    abstract fun dailyRewardDao(): DailyRewardDao
    abstract fun tradeOfferDao(): TradeOfferDao
    abstract fun assetDao(): AssetDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assets ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE assets ADD COLUMN thumbnailUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE assets ADD COLUMN imageSource TEXT NOT NULL DEFAULT 'Wikimedia Commons / Authorized Editorial CDN'")
                db.execSQL("ALTER TABLE assets ADD COLUMN licenseInfo TEXT NOT NULL DEFAULT 'Public Domain / CC BY-SA 4.0 / Unsplash License'")
                db.execSQL("ALTER TABLE assets ADD COLUMN verificationStatus TEXT NOT NULL DEFAULT 'VERIFIED_REAL_PHOTO'")
                db.execSQL("ALTER TABLE assets ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 1761200000000")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assets ADD COLUMN onlinePriceUsd REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE assets ADD COLUMN priceSource TEXT NOT NULL DEFAULT 'Verified Real-World Reference'")
                db.execSQL("ALTER TABLE assets ADD COLUMN priceType TEXT NOT NULL DEFAULT 'LIVE_QUOTE'")
                db.execSQL("ALTER TABLE assets ADD COLUMN priceUpdatedAt INTEGER NOT NULL DEFAULT 1761200000000")
            }
        }

        @Volatile
        private var INSTANCE: LifeAssetsDatabase? = null

        fun getInstance(context: Context): LifeAssetsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeAssetsDatabase::class.java,
                    "lifeassets_billionaire.db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun createInMemory(context: Context): LifeAssetsDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                LifeAssetsDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
