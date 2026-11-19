package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LifeOsDatabase
import com.example.data.local.entity.FinanceCategoryEntity
import com.example.data.local.entity.FinanceTransactionEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.security.CryptoManager
import com.example.domain.backup.BackupManager
import com.example.domain.backup.RestoreMode
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CryptoAndBackupTest {

    private lateinit var db: LifeOsDatabase
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LifeOsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepo = UserPreferencesRepository(context)
        backupManager = BackupManager(db, prefsRepo)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `password encryption and decryption round-trip works`() {
        val secret = "LIFE OS 365 private journal payload"
        val encrypted = CryptoManager.encryptWithPassword(secret, "strong-pass-123")
        assertFalse(encrypted.contains(secret))
        val decrypted = CryptoManager.decryptWithPassword(encrypted, "strong-pass-123")
        assertEquals(secret, decrypted)
    }

    @Test
    fun `encrypted JSON backup validates checksum, detects duplicates, and restores cleanly`() = runTest {
        db.habitDao().insertHabit(HabitEntity(name = "Morning Walk", category = "Fitness"))
        val catId = db.financeDao().insertCategory(FinanceCategoryEntity(name = "Books", type = "EXPENSE", monthlyBudget = 1500.0))
        db.financeDao().insertTransaction(
            FinanceTransactionEntity(
                categoryId = catId,
                title = "Clean Architecture",
                amount = 650.0,
                type = "EXPENSE",
                encryptedNote = CryptoManager.encryptField("Study book"),
                epochDay = 20_000L,
            ),
        )

        val exported = backupManager.exportToJson(password = "vault2026")
        assertTrue(backupManager.isBackupPasswordProtected(exported))

        val inspection = backupManager.validateAndInspectBackup(exported, password = "vault2026")
        assertEquals(1, inspection.duplicateCount) // Morning Walk matches existing habit

        backupManager.restoreBackup(inspection.payload, RestoreMode.REPLACE_ALL)
        assertEquals(1, db.habitDao().getAllHabitsOnce().size)
        assertEquals("Morning Walk", db.habitDao().getAllHabitsOnce().first().name)

        val csv = backupManager.exportFinanceCsv()
        assertTrue(csv.contains("Clean Architecture"))
        assertTrue(csv.contains("650.0"))
    }
}
