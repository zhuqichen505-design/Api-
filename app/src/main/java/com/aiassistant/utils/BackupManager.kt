package com.aiassistant.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.aiassistant.BuildConfig
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
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            BACKUP_DIR
        )
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

    // 恢复备份
    fun restoreBackup(context: Context, backupPath: String): Boolean {
        return try {
            val backupFile = File(backupPath)
            if (!backupFile.exists()) return false

            // 先创建当前数据的备份
            createBackup(context)

            ZipInputStream(FileInputStream(backupFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val fileName = entry.name
                    when {
                        fileName.startsWith("database/") -> {
                            val dbFile = context.getDatabasePath(fileName.removePrefix("database/"))
                            dbFile.parentFile?.mkdirs()
                            FileOutputStream(dbFile).use { out ->
                                zip.copyTo(out)
                            }
                        }
                        fileName.startsWith("files/") -> {
                            val file = File(context.filesDir, fileName.removePrefix("files/"))
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { out ->
                                zip.copyTo(out)
                            }
                        }
                        fileName.startsWith("shared_prefs/") -> {
                            val file = File(
                                File(context.applicationInfo.dataDir, "shared_prefs"),
                                fileName.removePrefix("shared_prefs/")
                            )
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { out ->
                                zip.copyTo(out)
                            }
                        }
                    }
                    entry = zip.nextEntry
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun restoreBackupFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val importDir = File(context.cacheDir, "backup_imports").apply { mkdirs() }
            val importFile = File(importDir, "import_${System.currentTimeMillis()}.zip")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(importFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return false
            restoreBackup(context, importFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 获取备份列表
    fun getBackupList(context: Context): List<BackupItem> {
        val backupDir = getBackupDir(context)
        if (!backupDir.exists()) return emptyList()

        return backupDir.listFiles()
            ?.filter { it.name.endsWith(".zip") }
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

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
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
            val backups = getBackupList(context)

            // 保留最近5个备份
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
            // 自动备份失败不影响应用运行
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

    data class BackupInfo(
        val version: Int,
        val timestamp: Long,
        val appVersion: String,
        val deviceInfo: String
    )

    data class BackupItem(
        val fileName: String,
        val filePath: String,
        val fileSize: Long,
        val lastModified: Long
    )
}
