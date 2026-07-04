package com.aiassistant

import android.app.Application
import android.util.Log
import com.aiassistant.data.local.AppDatabase
import com.aiassistant.data.repository.AiRepository
import com.aiassistant.utils.BackupManager
import com.aiassistant.utils.CryptoManager
import com.aiassistant.utils.PersonalizationManager
import com.aiassistant.utils.TavilySearchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AiAssistantApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var repository: AiRepository
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var cryptoManager: CryptoManager
        private set

    lateinit var personalizationManager: PersonalizationManager
        private set

    lateinit var tavilySearchManager: TavilySearchManager
        private set

    var isDatabaseInitialized = false
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        cryptoManager = CryptoManager(this)
        personalizationManager = PersonalizationManager(this)
        tavilySearchManager = TavilySearchManager(this, cryptoManager)

        // 先尝试自动备份
        try {
            BackupManager.autoBackup(this)
        } catch (e: Exception) {
            Log.w("AiAssistantApp", "Auto backup failed", e)
        }

        // 初始化数据库
        try {
            database = AppDatabase.getDatabase(this)
            repository = AiRepository(
                folderDao = database.folderDao(),
                apiConfigDao = database.apiConfigDao(),
                conversationDao = database.conversationDao(),
                messageDao = database.messageDao(),
                usageStatDao = database.usageStatDao(),
                environmentVariableDao = database.environmentVariableDao(),
                promptTemplateDao = database.promptTemplateDao(),
                memoryDao = database.memoryDao(),
                conversationBranchDao = database.conversationBranchDao(),
                selectedModelDao = database.selectedModelDao(),
                cryptoManager = cryptoManager,
                personalizationManager = personalizationManager,
                tavilySearchManager = tavilySearchManager
            )
            isDatabaseInitialized = true
        } catch (e: Exception) {
            Log.e("AiAssistantApp", "Database initialization failed", e)
            tryRestoreBackup()
        }
    }

    private fun tryRestoreBackup() {
        try {
            val backups = BackupManager.getBackupList(this)
            if (backups.isNotEmpty()) {
                val latestBackup = backups.first()
                if (BackupManager.restoreBackup(this, latestBackup.filePath)) {
                    database = AppDatabase.getDatabase(this)
                    repository = AiRepository(
                        folderDao = database.folderDao(),
                        apiConfigDao = database.apiConfigDao(),
                        conversationDao = database.conversationDao(),
                        messageDao = database.messageDao(),
                        usageStatDao = database.usageStatDao(),
                        environmentVariableDao = database.environmentVariableDao(),
                        promptTemplateDao = database.promptTemplateDao(),
                        memoryDao = database.memoryDao(),
                        conversationBranchDao = database.conversationBranchDao(),
                        selectedModelDao = database.selectedModelDao(),
                        cryptoManager = cryptoManager,
                        personalizationManager = personalizationManager,
                        tavilySearchManager = tavilySearchManager
                    )
                    isDatabaseInitialized = true
                }
            }
        } catch (e: Exception) {
            Log.e("AiAssistantApp", "Backup restore failed", e)
        }
    }

    companion object {
        lateinit var instance: AiAssistantApp
            private set
    }
}
