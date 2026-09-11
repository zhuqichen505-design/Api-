package com.aiassistant.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.aiassistant.BuildConfig
import com.aiassistant.data.local.AppDatabase
import com.aiassistant.domain.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {
    private const val BACKUP_DIR = "Echo_Backups"
    private const val DB_NAME = "ai_assistant_database"

    // 获取备份目录
    private fun getBackupDir(context: Context): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val dir = File(baseDir, BACKUP_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // 创建备份
    fun createBackup(context: Context): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "Echo_Backup_$timestamp.zip"
            val backupDir = getBackupDir(context)
            val backupFile = File(backupDir, backupFileName)

            val dbFile = context.getDatabasePath(DB_NAME)
            val dbWalFile = File(dbFile.path + "-wal")
            val dbShmFile = File(dbFile.path + "-shm")

            ZipOutputStream(FileOutputStream(backupFile)).use { zip ->
                // 备份数据库文件
                if (dbFile.exists()) {
                    addFileToZip(zip, dbFile, "database/$DB_NAME")
                }
                if (dbWalFile.exists()) {
                    addFileToZip(zip, dbWalFile, "database/$DB_NAME-wal")
                }
                if (dbShmFile.exists()) {
                    addFileToZip(zip, dbShmFile, "database/$DB_NAME-shm")
                }

                // 备份头像文件
                val avatarFile = File(context.filesDir, "user_avatar.dat")
                if (avatarFile.exists()) {
                    addFileToZip(zip, avatarFile, "files/user_avatar.dat")
                }
                val modelAvatarFile = File(context.filesDir, "model_avatar.dat")
                if (modelAvatarFile.exists()) {
                    addFileToZip(zip, modelAvatarFile, "files/model_avatar.dat")
                }
                context.filesDir.listFiles()
                    ?.filter { it.isFile && it.name.startsWith("model_avatar_api_") && it.name.endsWith(".dat") }
                    ?.forEach { file ->
                        addFileToZip(zip, file, "files/${file.name}")
                    }

                val personalizationFile = File(
                    File(context.applicationInfo.dataDir, "shared_prefs"),
                    "personalization_settings.xml"
                )
                if (personalizationFile.exists()) {
                    addFileToZip(zip, personalizationFile, "shared_prefs/personalization_settings.xml")
                }

                // 添加备份信息
                val info = BackupInfo(
                    version = 1,
                    timestamp = System.currentTimeMillis(),
                    appVersion = BuildConfig.VERSION_NAME,
                    deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                )
                val infoJson = GsonBuilder().setPrettyPrinting().create().toJson(info)
                zip.putNextEntry(ZipEntry("backup_info.json"))
                zip.write(infoJson.toByteArray())
                zip.closeEntry()
            }

            backupFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportBackupToUri(context: Context, uri: Uri): Boolean {
        return try {
            val backupPath = createBackup(context) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(File(backupPath)).use { input ->
                    input.copyTo(output)
                }
            } ?: return false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 备份单个对话为独立的结构化 JSON 备份文件
     * 包含完整的消息流、模型高级参数、角色卡、场景世界观与剧情记忆
     */
    fun createSingleConversationBackup(context: Context, conversationId: Long): String? {
        return try {
            val database = AppDatabase.getDatabase(context)
            val conversation = kotlinx.coroutines.runBlocking {
                database.conversationDao().getConversationById(conversationId)
            } ?: return null

            val messages = kotlinx.coroutines.runBlocking {
                database.messageDao().getMessagesList(conversationId)
            }

            val roleplaySession = kotlinx.coroutines.runBlocking {
                database.roleplaySessionDao().getSessionByConversationId(conversationId)
            }

            val characterProfile = roleplaySession?.characterId?.let { charId ->
                kotlinx.coroutines.runBlocking {
                    database.characterProfileDao().getCharacterById(charId)
                }
            }

            val roleplayScenario = roleplaySession?.scenarioId?.let { scenId ->
                kotlinx.coroutines.runBlocking {
                    database.roleplayScenarioDao().getScenarioById(scenId)
                }
            }

            val roleplayMemories = roleplaySession?.let { session ->
                kotlinx.coroutines.runBlocking {
                    database.roleplayMemoryDao().getMemoriesListBySession(session.id)
                }
            } ?: emptyList()

            val bundle = SingleConversationExport(
                formatVersion = 1,
                type = "single_conversation",
                exportedAt = System.currentTimeMillis(),
                appVersion = BuildConfig.VERSION_NAME,
                conversation = conversation,
                messages = messages,
                roleplaySession = roleplaySession,
                characterProfile = characterProfile,
                roleplayScenario = roleplayScenario,
                roleplayMemories = roleplayMemories
            )

            val json = GsonBuilder().setPrettyPrinting().create().toJson(bundle)
            val backupDir = getBackupDir(context)
            val safeTitle = conversation.title
                .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
                .trim('_')
                .take(30)
                .ifBlank { "对话_${conversation.id}" }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Echo_Backup_Conv_${safeTitle}_$timestamp.json"
            val backupFile = File(backupDir, fileName)
            backupFile.writeText(json, Charsets.UTF_8)
            backupFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 智能非破坏性恢复备份：
     * 自动识别全量 zip 备份与单对话 json 备份。
     * 绝不删除当前数据库中未包含在备份中的任何已有对话！
     */
    fun restoreBackup(context: Context, backupPath: String): Boolean {
        return try {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) return false

            // 先创建当前数据的安全备份
            createBackup(context)

            if (backupFile.name.endsWith(".json", ignoreCase = true)) {
                val content = backupFile.readText(Charsets.UTF_8)
                return restoreSingleConversationFromJson(context, content)
            }

            // 针对 ZIP 格式备份
            val tempDir = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}").apply { mkdirs() }
            var tempDbFile: File? = null
            var singleJsonContent: String? = null

            try {
                ZipInputStream(FileInputStream(backupFile)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val fileName = entry.name
                        when {
                            fileName.startsWith("database/") -> {
                                val target = File(tempDir, fileName.removePrefix("database/"))
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { out -> zip.copyTo(out) }
                                if (target.name == DB_NAME) {
                                    tempDbFile = target
                                }
                            }
                            fileName.endsWith(".json") && !fileName.contains("backup_info") -> {
                                val target = File(tempDir, fileName)
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { out -> zip.copyTo(out) }
                                if (singleJsonContent == null) {
                                    singleJsonContent = target.readText(Charsets.UTF_8)
                                }
                            }
                            fileName.startsWith("files/") -> {
                                val file = File(context.filesDir, fileName.removePrefix("files/"))
                                file.parentFile?.mkdirs()
                                FileOutputStream(file).use { out -> zip.copyTo(out) }
                            }
                            fileName.startsWith("shared_prefs/") -> {
                                val file = File(
                                    File(context.applicationInfo.dataDir, "shared_prefs"),
                                    fileName.removePrefix("shared_prefs/")
                                )
                                file.parentFile?.mkdirs()
                                FileOutputStream(file).use { out -> zip.copyTo(out) }
                            }
                        }
                        entry = zip.nextEntry
                    }
                }

                if (singleJsonContent != null && singleJsonContent.contains("\"single_conversation\"")) {
                    return restoreSingleConversationFromJson(context, singleJsonContent)
                }

                if (tempDbFile != null && tempDbFile!!.exists()) {
                    return mergeDatabaseFromBackup(context, tempDbFile!!)
                }

                false
            } finally {
                tempDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 单对话非破坏性恢复：仅插入或更新对应会话与关联消息，其它所有会话 100% 保留
     */
    fun restoreSingleConversationFromJson(context: Context, jsonString: String): Boolean {
        return try {
            val bundle = try {
                Gson().fromJson(jsonString, SingleConversationExport::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } ?: return false

            val database = AppDatabase.getDatabase(context)
            kotlinx.coroutines.runBlocking {
                database.withTransaction {
                    val existing = database.conversationDao().getConversationById(bundle.conversation.id)
                    val targetConvId = if (existing == null) {
                        database.conversationDao().insertConversation(bundle.conversation)
                    } else if (existing.title == bundle.conversation.title && existing.createdAt == bundle.conversation.createdAt) {
                        database.conversationDao().updateConversation(bundle.conversation)
                        database.messageDao().deleteMessagesByConversation(existing.id)
                        existing.id
                    } else {
                        database.conversationDao().insertConversation(bundle.conversation.copy(id = 0L))
                    }

                    val characterIdMap = mutableMapOf<Long, Long>()
                    bundle.characterProfile?.let { charProfile ->
                        val existingChar = database.characterProfileDao().getCharacterById(charProfile.id)
                        val targetCharId = if (existingChar == null) {
                            database.characterProfileDao().insertCharacter(charProfile)
                        } else if (existingChar.name == charProfile.name) {
                            database.characterProfileDao().updateCharacter(charProfile)
                            charProfile.id
                        } else {
                            database.characterProfileDao().insertCharacter(charProfile.copy(id = 0L))
                        }
                        characterIdMap[charProfile.id] = targetCharId
                    }

                    val scenarioIdMap = mutableMapOf<Long, Long>()
                    bundle.roleplayScenario?.let { scenario ->
                        val existingScen = database.roleplayScenarioDao().getScenarioById(scenario.id)
                        val targetScenId = if (existingScen == null) {
                            database.roleplayScenarioDao().insertScenario(scenario)
                        } else if (existingScen.name == scenario.name) {
                            database.roleplayScenarioDao().updateScenario(scenario)
                            scenario.id
                        } else {
                            database.roleplayScenarioDao().insertScenario(scenario.copy(id = 0L))
                        }
                        scenarioIdMap[scenario.id] = targetScenId
                    }

                    var targetSessionId: Long? = null
                    bundle.roleplaySession?.let { session ->
                        val remappedCharId = session.characterId?.let { characterIdMap[it] ?: it }
                        val remappedScenId = session.scenarioId?.let { scenarioIdMap[it] ?: it }
                        val existingSession = database.roleplaySessionDao().getSessionByConversationId(targetConvId)
                        val newSession = session.copy(
                            id = existingSession?.id ?: 0L,
                            conversationId = targetConvId,
                            characterId = remappedCharId,
                            scenarioId = remappedScenId
                        )
                        targetSessionId = database.roleplaySessionDao().insertSession(newSession)
                    }

                    if (targetSessionId != null && bundle.roleplayMemories.isNotEmpty()) {
                        bundle.roleplayMemories.forEach { mem ->
                            database.roleplayMemoryDao().insertMemory(
                                mem.copy(
                                    id = 0L,
                                    sessionId = targetSessionId!!
                                )
                            )
                        }
                    }

                    if (bundle.messages.isNotEmpty()) {
                        val messagesToInsert = bundle.messages.map { msg ->
                            msg.copy(
                                id = 0L,
                                conversationId = targetConvId
                            )
                        }
                        database.messageDao().insertMessages(messagesToInsert)
                    }

                    database.conversationDao().updateStats(
                        id = targetConvId,
                        count = bundle.messages.size,
                        tokens = bundle.messages.sumOf { it.tokenCount }
                    )
                }
            }
            database.invalidationTracker.refreshVersionsSync()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 全量 SQLite 数据库增量合并引擎：
     * 逐表检查与插入，未包含在备份包中的已有本地数据绝对不被删除！
     */
    fun mergeDatabaseFromBackup(context: Context, tempDbFile: File): Boolean {
        if (!tempDbFile.exists()) return false
        val tempDb = try {
            android.database.sqlite.SQLiteDatabase.openDatabase(
                tempDbFile.path,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        val database = AppDatabase.getDatabase(context)
        val activeDb = database.openHelper.writableDatabase

        return try {
            activeDb.beginTransaction()
            try {
                // 1. Folders
                if (tableExists(tempDb, "folders") && tableExists(activeDb, "folders")) {
                    tempDb.rawQuery("SELECT * FROM folders", null).use { cursor ->
                        val colNames = cursor.columnNames.toList()
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                            var exists = false
                            activeDb.query("SELECT id FROM folders WHERE id = ? OR name = ?", arrayOf(id, name)).use { c ->
                                if (c.moveToFirst()) exists = true
                            }
                            if (!exists) {
                                val cv = ContentValues()
                                colNames.forEach { col -> putColumnValue(cv, cursor, col) }
                                activeDb.insert("folders", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, cv)
                            }
                        }
                    }
                }

                // 2. ApiConfigs
                if (tableExists(tempDb, "api_configs") && tableExists(activeDb, "api_configs")) {
                    tempDb.rawQuery("SELECT * FROM api_configs", null).use { cursor ->
                        val colNames = cursor.columnNames.toList()
                        while (cursor.moveToNext()) {
                            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                            val baseUrl = cursor.getString(cursor.getColumnIndexOrThrow("baseUrl"))
                            var exists = false
                            activeDb.query("SELECT id FROM api_configs WHERE name = ? AND baseUrl = ?", arrayOf(name, baseUrl)).use { c ->
                                if (c.moveToFirst()) exists = true
                            }
                            if (!exists) {
                                val cv = ContentValues()
                                colNames.forEach { col -> putColumnValue(cv, cursor, col) }
                                cv.put("isDefault", 0)
                                activeDb.insert("api_configs", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, cv)
                            }
                        }
                    }
                }

                // 3. CharacterProfiles
                val characterIdMap = mutableMapOf<Long, Long>()
                if (tableExists(tempDb, "character_profiles") && tableExists(activeDb, "character_profiles")) {
                    tempDb.rawQuery("SELECT * FROM character_profiles", null).use { cursor ->
                        val colNames = cursor.columnNames.toList()
                        while (cursor.moveToNext()) {
                            val backupCharId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                            var existingId: Long? = null
                            activeDb.query("SELECT id FROM character_profiles WHERE id = ? OR name = ?", arrayOf(backupCharId, name)).use { c ->
                                if (c.moveToFirst()) existingId = c.getLong(0)
                            }
                            if (existingId != null) {
                                characterIdMap[backupCharId] = existingId!!
                            } else {
                                val cv = ContentValues()
                                colNames.forEach { col -> putColumnValue(cv, cursor, col) }
                                val newId = activeDb.insert("character_profiles", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, cv)
                                characterIdMap[backupCharId] = if (newId > 0) newId else backupCharId
                            }
                        }
                    }
                }

                // 4. RoleplayScenarios
                val scenarioIdMap = mutableMapOf<Long, Long>()
                if (tableExists(tempDb, "roleplay_scenarios") && tableExists(activeDb, "roleplay_scenarios")) {
                    tempDb.rawQuery("SELECT * FROM roleplay_scenarios", null).use { cursor ->
                        val colNames = cursor.columnNames.toList()
                        while (cursor.moveToNext()) {
                            val backupScenId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                            var existingId: Long? = null
                            activeDb.query("SELECT id FROM roleplay_scenarios WHERE id = ? OR name = ?", arrayOf(backupScenId, name)).use { c ->
                                if (c.moveToFirst()) existingId = c.getLong(0)
                            }
                            if (existingId != null) {
                                scenarioIdMap[backupScenId] = existingId!!
                            } else {
                                val cv = ContentValues()
                                colNames.forEach { col -> putColumnValue(cv, cursor, col) }
                                val newId = activeDb.insert("roleplay_scenarios", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, cv)
                                scenarioIdMap[backupScenId] = if (newId > 0) newId else backupScenId
                            }
                        }
                    }
                }

                // 5. Conversations
                val conversationIdMap = mutableMapOf<Long, Long>()
                if (tableExists(tempDb, "conversations") && tableExists(activeDb, "conversations")) {
                    tempDb.rawQuery("SELECT * FROM conversations", null).use { cursor ->
                        val colNames = cursor.columnNames.toList()
                        while (cursor.moveToNext()) {
                            val backupConvId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                            val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                            val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))

                            var existingCreatedAt: Long? = null
                            var existingTitle: String? = null
                            activeDb.query("SELECT id, createdAt, title FROM conversations WHERE id = ?", arrayOf(backupConvId)).use { c ->
                                if (c.moveToFirst()) {
                                    existingCreatedAt = c.getLong(1)
                                    existingTitle = c.getString(2)
                                }
                            }

                            if (existingCreatedAt == null) {
                                val cv = ContentValues()
                                colNames.forEach { col -> putColumnValue(cv, cursor, col) }
                                activeDb.insert("conversations", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv)
                                conversationIdMap[backupConvId] = backupConvId
                            } else if (existingCreatedAt == createdAt && existingTitle == title) {
                                val cv = ContentValues()
                                colNames.forEach { col -> putColumnValue(cv, cursor, col) }
                                activeDb.update("conversations", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv, "id = ?", arrayOf(backupConvId.toString()))
                                conversationIdMap[backupConvId] = backupConvId
                            } else {
                                val cv = ContentValues()
                                colNames.filter { it != "id" }.forEach { col -> putColumnValue(cv, cursor, col) }
                                val allocatedId = activeDb.insert("conversations", android.database.sqlite.SQLiteDatabase.CONFLICT_NONE, cv)
                                conversationIdMap[backupConvId] = allocatedId
                            }
                        }
                    }
                }

                // 6. Messages
                if (tableExists(tempDb, "messages") && tableExists(activeDb, "messages")) {
                    tempDb.rawQuery("SELECT * FROM messages", null).use { cursor ->
                        val colNames = cursor.columnNames.toList()
                        while (cursor.moveToNext()) {
                            val backupConvId = cursor.getLong(cursor.getColumnIndexOrThrow("conversationId"))
                            val activeConvId = conversationIdMap[backupConvId]
                            if (activeConvId != null) {
                                val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
                                val role = cursor.getString(cursor.getColumnIndexOrThrow("role"))
                                var msgExists = false
                                activeDb.query("SELECT id FROM messages WHERE conversationId = ? AND createdAt = ? AND role = ?", arrayOf(activeConvId, createdAt, role)).use { c ->
                                    if (c.moveToFirst()) msgExists = true
                                }
                                if (!msgExists) {
                                    val cv = ContentValues()
                                    colNames.filter { it != "id" }.forEach { col -> putColumnValue(cv, cursor, col) }
                                    cv.put("conversationId", activeConvId)
                                    activeDb.insert("messages", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, cv)
                                }
                            }
                        }
                    }
                }

                // 7. Roleplay Sessions & Memories
                val sessionIdMap = mutableMapOf<Long, Long>()
                if (tableExists(tempDb, "roleplay_sessions") && tableExists(activeDb, "roleplay_sessions")) {
                    tempDb.rawQuery("SELECT * FROM roleplay_sessions", null).use { cursor ->
                        val colNames = cursor.columnNames.toList()
                        while (cursor.moveToNext()) {
                            val backupSessionId = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                            val backupConvId = cursor.getLong(cursor.getColumnIndexOrThrow("conversationId"))
                            val activeConvId = conversationIdMap[backupConvId]
                            if (activeConvId != null) {
                                val charId = if (cursor.isNull(cursor.getColumnIndexOrThrow("characterId"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("characterId"))
                                val scenId = if (cursor.isNull(cursor.getColumnIndexOrThrow("scenarioId"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("scenarioId"))
                                val mappedCharId = charId?.let { characterIdMap[it] ?: it }
                                val mappedScenId = scenId?.let { scenarioIdMap[it] ?: it }

                                var existingSessionId: Long? = null
                                activeDb.query("SELECT id FROM roleplay_sessions WHERE conversationId = ?", arrayOf(activeConvId)).use { c ->
                                    if (c.moveToFirst()) existingSessionId = c.getLong(0)
                                }

                                val cv = ContentValues()
                                colNames.filter { it != "id" }.forEach { col -> putColumnValue(cv, cursor, col) }
                                cv.put("conversationId", activeConvId)
                                if (mappedCharId != null) cv.put("characterId", mappedCharId)
                                if (mappedScenId != null) cv.put("scenarioId", mappedScenId)

                                if (existingSessionId != null) {
                                    activeDb.update("roleplay_sessions", android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, cv, "id = ?", arrayOf(existingSessionId.toString()))
                                    sessionIdMap[backupSessionId] = existingSessionId!!
                                } else {
                                    val allocatedSessionId = activeDb.insert("roleplay_sessions", android.database.sqlite.SQLiteDatabase.CONFLICT_NONE, cv)
                                    sessionIdMap[backupSessionId] = allocatedSessionId
                                }
                            }
                        }
                    }
                }

                if (tableExists(tempDb, "roleplay_memories") && tableExists(activeDb, "roleplay_memories")) {
                    tempDb.rawQuery("SELECT * FROM roleplay_memories", null).use { cursor ->
                        val colNames = cursor.columnNames.toList()
                        while (cursor.moveToNext()) {
                            val backupSessionId = cursor.getLong(cursor.getColumnIndexOrThrow("sessionId"))
                            val activeSessionId = sessionIdMap[backupSessionId]
                            if (activeSessionId != null) {
                                val cv = ContentValues()
                                colNames.filter { it != "id" }.forEach { col -> putColumnValue(cv, cursor, col) }
                                cv.put("sessionId", activeSessionId)
                                activeDb.insert("roleplay_memories", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, cv)
                            }
                        }
                    }
                }

                // 8. Other supplementary tables
                val simpleTables = listOf("prompt_templates", "memory_items", "conversation_branches", "selected_models")
                for (tableName in simpleTables) {
                    if (tableExists(tempDb, tableName) && tableExists(activeDb, tableName)) {
                        tempDb.rawQuery("SELECT * FROM $tableName", null).use { cursor ->
                            val colNames = cursor.columnNames.toList()
                            while (cursor.moveToNext()) {
                                val cv = ContentValues()
                                colNames.filter { it != "id" }.forEach { col -> putColumnValue(cv, cursor, col) }
                                activeDb.insert(tableName, android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, cv)
                            }
                        }
                    }
                }

                activeDb.setTransactionSuccessful()
                true
            } finally {
                activeDb.endTransaction()
                tempDb.close()
                database.invalidationTracker.refreshVersionsSync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tempDb.close()
            false
        }
    }

    private fun tableExists(db: android.database.sqlite.SQLiteDatabase, tableName: String): Boolean {
        return try {
            db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use {
                it.moveToFirst()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun tableExists(db: androidx.sqlite.db.SupportSQLiteDatabase, tableName: String): Boolean {
        return try {
            db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tableName)).use {
                it.moveToFirst()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun putColumnValue(cv: ContentValues, cursor: Cursor, col: String) {
        val idx = cursor.getColumnIndex(col)
        if (idx == -1 || cursor.isNull(idx)) {
            cv.putNull(col)
            return
        }
        when (cursor.getType(idx)) {
            Cursor.FIELD_TYPE_INTEGER -> cv.put(col, cursor.getLong(idx))
            Cursor.FIELD_TYPE_FLOAT -> cv.put(col, cursor.getDouble(idx))
            Cursor.FIELD_TYPE_STRING -> cv.put(col, cursor.getString(idx))
            Cursor.FIELD_TYPE_BLOB -> cv.put(col, cursor.getBlob(idx))
            else -> cv.put(col, cursor.getString(idx))
        }
    }

    fun restoreBackupFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val importDir = File(context.cacheDir, "backup_imports").apply { mkdirs() }
            var fileName = "import_${System.currentTimeMillis()}"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        fileName = displayName
                    }
                }
            }
            if (!fileName.endsWith(".zip", ignoreCase = true) && !fileName.endsWith(".json", ignoreCase = true)) {
                fileName += ".zip"
            }
            val importFile = File(importDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(importFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return false
            val result = restoreBackup(context, importFile.absolutePath)
            importFile.delete()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 获取备份列表（包含全量 .zip 与单对话 .json）
    fun getBackupList(context: Context): List<BackupItem> {
        val backupDir = getBackupDir(context)
        if (!backupDir.exists()) return emptyList()

        return backupDir.listFiles()
            ?.filter { it.name.endsWith(".zip", ignoreCase = true) || it.name.endsWith(".json", ignoreCase = true) }
            ?.map { file ->
                BackupItem(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    lastModified = file.lastModified()
                )
            }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    // 删除备份
    fun deleteBackup(backupPath: String): Boolean {
        return try {
            File(backupPath).delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    // 分享备份文件
    fun shareBackup(context: Context, backupPath: String) {
        try {
            val file = File(backupPath)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = if (file.name.endsWith(".json", ignoreCase = true)) {
                "application/json"
            } else {
                "application/zip"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "分享备份文件"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 自动备份（应用启动时调用）
    fun autoBackup(context: Context) {
        try {
            val backups = getBackupList(context).filter { it.fileName.endsWith(".zip") }

            // 保留最近5个全量备份
            if (backups.size > 5) {
                backups.drop(5).forEach { backup ->
                    deleteBackup(backup.filePath)
                }
            }

            // 每天最多自动备份一次
            val lastBackup = backups.firstOrNull()
            val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val lastBackupDate = lastBackup?.let {
                SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.lastModified))
            }

            if (lastBackupDate != today) {
                createBackup(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { input ->
            input.copyTo(zip)
        }
        zip.closeEntry()
    }

    data class SingleConversationExport(
        val formatVersion: Int = 1,
        val type: String = "single_conversation",
        val exportedAt: Long = System.currentTimeMillis(),
        val appVersion: String = BuildConfig.VERSION_NAME,
        val conversation: Conversation,
        val messages: List<Message>,
        val roleplaySession: RoleplaySession? = null,
        val characterProfile: CharacterProfile? = null,
        val roleplayScenario: RoleplayScenario? = null,
        val roleplayMemories: List<RoleplayMemory> = emptyList()
    )

    data class BackupInfo(
        val version: Int,
        val timestamp: Long,
        val appVersion: String,
        val deviceInfo: String,
        val includeRoleplayData: Boolean = true,
        val roleplayCharacterCount: Int = 0,
        val roleplayScenarioCount: Int = 0,
        val roleplaySessionCount: Int = 0
    )

    data class BackupItem(
        val fileName: String,
        val filePath: String,
        val fileSize: Long,
        val lastModified: Long
    )
}
