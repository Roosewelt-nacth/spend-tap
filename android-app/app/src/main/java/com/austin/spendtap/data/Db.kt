package com.austin.spendtap.data

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
import com.austin.spendtap.model.SavingsKind
import com.austin.spendtap.model.SavingsPlan
import com.austin.spendtap.model.SpendEntry
import kotlinx.coroutines.flow.Flow

// Columns mirror the Supabase `entries` table so a later sync is a straight copy.

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val category: String,
    val note: String,
    val paymentMethod: String,
    val isImpulse: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val monthlyAmount: Double,
    val dayOfMonth: Int,
    val startDate: Long,
    val endDate: Long?,
    val targetAmount: Double?,
    val expectedReturnPct: Double,
    val isActive: Boolean,
)

fun EntryEntity.toModel() = SpendEntry(id, amount, category, note, paymentMethod, isImpulse, createdAt)
fun SpendEntry.toEntity() = EntryEntity(id, amount, category, note, paymentMethod, isImpulse, createdAt)

fun PlanEntity.toModel() = SavingsPlan(
    id = id, name = name,
    kind = runCatching { SavingsKind.valueOf(kind) }.getOrDefault(SavingsKind.OTHER),
    monthlyAmount = monthlyAmount, dayOfMonth = dayOfMonth, startDate = startDate, endDate = endDate,
    targetAmount = targetAmount, expectedReturnPct = expectedReturnPct, isActive = isActive,
)

fun SavingsPlan.toEntity() = PlanEntity(
    id, name, kind.name, monthlyAmount, dayOfMonth, startDate, endDate, targetAmount, expectedReturnPct, isActive,
)

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY createdAt DESC")
    fun all(): Flow<List<EntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY name")
    fun all(): Flow<List<PlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: PlanEntity)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [EntryEntity::class, PlanEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun planDao(): PlanDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "spendtap.db").build()
    }
}
