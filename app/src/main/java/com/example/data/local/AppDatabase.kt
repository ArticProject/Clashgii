package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recent_opponents")
data class RecentOpponentEntity(
    @PrimaryKey val tag: String,
    val name: String,
    val clanName: String?,
    val trophies: Int,
    val avgElixir: Double,
    val deckJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "card_catalog")
data class CardCatalogEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val elixirCost: Int,
    val maxLevel: Int,
    val iconUrl: String,
    val evolutionIconUrl: String?
)

@Dao
interface RoyaleDao {
    @Query("SELECT * FROM recent_opponents ORDER BY updatedAt DESC LIMIT 20")
    fun observeRecentOpponents(): Flow<List<RecentOpponentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentOpponent(entity: RecentOpponentEntity)

    @Query("DELETE FROM recent_opponents WHERE tag = :tag")
    suspend fun deleteRecentOpponent(tag: String)

    @Query("SELECT * FROM card_catalog ORDER BY elixirCost ASC, name ASC")
    fun observeCardCatalog(): Flow<List<CardCatalogEntity>>

    @Query("SELECT * FROM card_catalog")
    suspend fun getAllCards(): List<CardCatalogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCards(cards: List<CardCatalogEntity>)
}

@Database(
    entities = [RecentOpponentEntity::class, CardCatalogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun royaleDao(): RoyaleDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "royalepulse_db"
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
}
