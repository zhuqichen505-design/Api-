package com.aiassistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aiassistant.domain.model.*

@Database(
    entities = [
        Folder::class,
        ApiConfig::class,
        Conversation::class,
        Message::class,
        ApiUsageStat::class,
        EnvironmentVariable::class,
        PromptTemplate::class,
        ConversationBranch::class,
        SelectedModel::class
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun apiConfigDao(): ApiConfigDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun usageStatDao(): UsageStatDao
    abstract fun environmentVariableDao(): EnvironmentVariableDao
    abstract fun promptTemplateDao(): PromptTemplateDao
    abstract fun conversationBranchDao(): ConversationBranchDao
    abstract fun selectedModelDao(): SelectedModelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `folders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon` TEXT NOT NULL DEFAULT 'folder',
                        `color` INTEGER NOT NULL DEFAULT 0,
                        `parentId` INTEGER,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_folders_parentId` ON `folders` (`parentId`)")
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `topK` INTEGER NOT NULL DEFAULT 50")
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `frequencyPenalty` REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `presencePenalty` REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `enableThinking` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `thinkingBudget` INTEGER NOT NULL DEFAULT 1024")
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `thinkingEffort` TEXT NOT NULL DEFAULT 'medium'")
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `stopSequences` TEXT")
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `seed` INTEGER")
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `responseFormat` TEXT")
                database.execSQL("ALTER TABLE `conversations` ADD COLUMN `folderId` INTEGER")
                database.execSQL("ALTER TABLE `conversations` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `conversations` ADD COLUMN `tags` TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_folderId` ON `conversations` (`folderId`)")
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `thinkingContent` TEXT")
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `thinkingTokens` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversationId` ON `messages` (`conversationId`)")
                database.execSQL("ALTER TABLE `api_usage_stats` ADD COLUMN `thinkingTokens` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `enableWebSearch` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `searchContextSize` TEXT NOT NULL DEFAULT 'medium'")
                database.execSQL("ALTER TABLE `messages` ADD COLUMN `attachments` TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `apiType` TEXT NOT NULL DEFAULT 'openai'")
                database.execSQL("ALTER TABLE `api_configs` ADD COLUMN `availableModels` TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `prompt_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `description` TEXT,
                        `category` TEXT NOT NULL DEFAULT 'general',
                        `variables` TEXT,
                        `isBuiltIn` INTEGER NOT NULL DEFAULT 0,
                        `useCount` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_prompt_templates_category` ON `prompt_templates` (`category`)")
            }
        }

        // 从任意旧版本迁移到版本10（空迁移，只是提高版本号）
        private val MIGRATION_5_10 = object : Migration(5, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }
        private val MIGRATION_6_10 = object : Migration(6, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }
        private val MIGRATION_7_10 = object : Migration(7, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }
        private val MIGRATION_8_10 = object : Migration(8, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }

        // 从版本10迁移到版本11 - 添加新表
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建会话分支表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `conversation_branches` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `parentConversationId` INTEGER NOT NULL,
                        `branchMessageId` INTEGER NOT NULL,
                        `childConversationId` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_conversation_branches_parentConversationId` ON `conversation_branches` (`parentConversationId`)")

                // 创建选择的模型表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `selected_models` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `apiConfigId` INTEGER NOT NULL,
                        `modelName` TEXT NOT NULL,
                        `displayName` TEXT,
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_selected_models_apiConfigId` ON `selected_models` (`apiConfigId`)")

                // 给api_usage_stats表添加缓存token字段
                addColumnIfMissing(database, "api_usage_stats", "cachedTokens", "INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 从版本11迁移到版本12 - 添加对话级别配置
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                addConversationSettingsColumns(database)
            }
        }

        // 从版本12迁移到版本13 - 修复历史版本空迁移可能留下的缺失字段。
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                addColumnIfMissing(database, "api_configs", "apiType", "TEXT NOT NULL DEFAULT 'openai'")
                addColumnIfMissing(database, "api_configs", "availableModels", "TEXT")
                addColumnIfMissing(database, "api_configs", "topK", "INTEGER NOT NULL DEFAULT 50")
                addColumnIfMissing(database, "api_configs", "frequencyPenalty", "REAL NOT NULL DEFAULT 0.0")
                addColumnIfMissing(database, "api_configs", "presencePenalty", "REAL NOT NULL DEFAULT 0.0")
                addColumnIfMissing(database, "api_configs", "enableThinking", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, "api_configs", "thinkingBudget", "INTEGER NOT NULL DEFAULT 1024")
                addColumnIfMissing(database, "api_configs", "thinkingEffort", "TEXT NOT NULL DEFAULT 'medium'")
                addColumnIfMissing(database, "api_configs", "enableWebSearch", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, "api_configs", "searchContextSize", "TEXT NOT NULL DEFAULT 'medium'")
                addColumnIfMissing(database, "api_configs", "stopSequences", "TEXT")
                addColumnIfMissing(database, "api_configs", "seed", "INTEGER")
                addColumnIfMissing(database, "api_configs", "responseFormat", "TEXT")

                addColumnIfMissing(database, "messages", "thinkingContent", "TEXT")
                addColumnIfMissing(database, "messages", "thinkingTokens", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, "messages", "attachments", "TEXT")

                addColumnIfMissing(database, "api_usage_stats", "inputTokens", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, "api_usage_stats", "outputTokens", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, "api_usage_stats", "thinkingTokens", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, "api_usage_stats", "cachedTokens", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, "api_usage_stats", "responseTime", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, "api_usage_stats", "errorMessage", "TEXT")

                addColumnIfMissing(database, "conversations", "folderId", "INTEGER")
                addColumnIfMissing(database, "conversations", "isPinned", "INTEGER NOT NULL DEFAULT 0")
                addColumnIfMissing(database, "conversations", "tags", "TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_folderId` ON `conversations` (`folderId`)")
                addConversationSettingsColumns(database)
            }
        }

        private fun addConversationSettingsColumns(database: SupportSQLiteDatabase) {
            addColumnIfMissing(database, "conversations", "temperature", "REAL")
            addColumnIfMissing(database, "conversations", "maxTokens", "INTEGER")
            addColumnIfMissing(database, "conversations", "topP", "REAL")
            addColumnIfMissing(database, "conversations", "enableThinking", "INTEGER")
            addColumnIfMissing(database, "conversations", "thinkingEffort", "TEXT")
            addColumnIfMissing(database, "conversations", "enableWebSearch", "INTEGER")
        }

        private fun addColumnIfMissing(
            database: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            definition: String
        ) {
            if (!hasColumn(database, tableName, columnName)) {
                database.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $definition")
            }
        }

        private fun hasColumn(
            database: SupportSQLiteDatabase,
            tableName: String,
            columnName: String
        ): Boolean {
            database.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == columnName) return true
                }
                return false
            }
        }

        private val LEGACY_REPAIR_MIGRATIONS: Array<Migration> = (1..15)
            .map { startVersion ->
                object : Migration(startVersion, 16) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        repairSchema(database)
                    }
                }
            }
            .toTypedArray()

        private fun repairSchema(database: SupportSQLiteDatabase) {
            repairTable(
                database,
                tableName = "folders",
                columns = listOf(
                    ColumnSpec("id", "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL", "0"),
                    ColumnSpec("name", "TEXT NOT NULL", "'未命名'"),
                    ColumnSpec("icon", "TEXT NOT NULL", "'folder'"),
                    ColumnSpec("color", "INTEGER NOT NULL", "0"),
                    ColumnSpec("parentId", "INTEGER", "NULL", nullable = true),
                    ColumnSpec("sortOrder", "INTEGER NOT NULL", "0"),
                    ColumnSpec("createdAt", "INTEGER NOT NULL", "0"),
                    ColumnSpec("updatedAt", "INTEGER NOT NULL", "0")
                ),
                indices = listOf("CREATE INDEX IF NOT EXISTS `index_folders_parentId` ON `folders` (`parentId`)")
            )
            repairTable(
                database,
                tableName = "api_configs",
                columns = listOf(
                    ColumnSpec("id", "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL", "0"),
                    ColumnSpec("name", "TEXT NOT NULL", "'API配置'"),
                    ColumnSpec("provider", "TEXT NOT NULL", "'OpenAI'"),
                    ColumnSpec("baseUrl", "TEXT NOT NULL", "''"),
                    ColumnSpec("apiKey", "TEXT NOT NULL", "''"),
                    ColumnSpec("apiType", "TEXT NOT NULL", "'openai'"),
                    ColumnSpec("modelName", "TEXT NOT NULL", "'gpt-4o-mini'"),
                    ColumnSpec("availableModels", "TEXT", "NULL", nullable = true),
                    ColumnSpec("maxTokens", "INTEGER NOT NULL", "4096"),
                    ColumnSpec("temperature", "REAL NOT NULL", "0.95"),
                    ColumnSpec("topP", "REAL NOT NULL", "1.0"),
                    ColumnSpec("topK", "INTEGER NOT NULL", "50"),
                    ColumnSpec("frequencyPenalty", "REAL NOT NULL", "0.0"),
                    ColumnSpec("presencePenalty", "REAL NOT NULL", "0.0"),
                    ColumnSpec("enableThinking", "INTEGER NOT NULL", "0"),
                    ColumnSpec("thinkingBudget", "INTEGER NOT NULL", "1024"),
                    ColumnSpec("thinkingEffort", "TEXT NOT NULL", "'medium'"),
                    ColumnSpec("enableWebSearch", "INTEGER NOT NULL", "0"),
                    ColumnSpec("searchContextSize", "TEXT NOT NULL", "'medium'"),
                    ColumnSpec("stopSequences", "TEXT", "NULL", nullable = true),
                    ColumnSpec("seed", "INTEGER", "NULL", nullable = true),
                    ColumnSpec("responseFormat", "TEXT", "NULL", nullable = true),
                    ColumnSpec("isDefault", "INTEGER NOT NULL", "0"),
                    ColumnSpec("createdAt", "INTEGER NOT NULL", "0"),
                    ColumnSpec("updatedAt", "INTEGER NOT NULL", "0")
                )
            )
            repairTable(
                database,
                tableName = "conversations",
                columns = listOf(
                    ColumnSpec("id", "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL", "0"),
                    ColumnSpec("title", "TEXT NOT NULL", "'新对话'"),
                    ColumnSpec("folderId", "INTEGER", "NULL", nullable = true),
                    ColumnSpec("apiConfigId", "INTEGER NOT NULL", "0"),
                    ColumnSpec("modelName", "TEXT NOT NULL", "''"),
                    ColumnSpec("systemPrompt", "TEXT", "NULL", nullable = true),
                    ColumnSpec("totalTokens", "INTEGER NOT NULL", "0"),
                    ColumnSpec("messageCount", "INTEGER NOT NULL", "0"),
                    ColumnSpec("isPinned", "INTEGER NOT NULL", "0"),
                    ColumnSpec("tags", "TEXT", "NULL", nullable = true),
                    ColumnSpec("temperature", "REAL", "NULL", nullable = true),
                    ColumnSpec("maxTokens", "INTEGER", "NULL", nullable = true),
                    ColumnSpec("topP", "REAL", "NULL", nullable = true),
                    ColumnSpec("enableThinking", "INTEGER", "NULL", nullable = true),
                    ColumnSpec("thinkingEffort", "TEXT", "NULL", nullable = true),
                    ColumnSpec("enableWebSearch", "INTEGER", "NULL", nullable = true),
                    ColumnSpec("createdAt", "INTEGER NOT NULL", "0"),
                    ColumnSpec("updatedAt", "INTEGER NOT NULL", "0")
                ),
                indices = listOf("CREATE INDEX IF NOT EXISTS `index_conversations_folderId` ON `conversations` (`folderId`)")
            )
            repairTable(
                database,
                tableName = "messages",
                columns = listOf(
                    ColumnSpec("id", "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL", "0"),
                    ColumnSpec("conversationId", "INTEGER NOT NULL", "0"),
                    ColumnSpec("role", "TEXT NOT NULL", "'user'"),
                    ColumnSpec("content", "TEXT NOT NULL", "''"),
                    ColumnSpec("thinkingContent", "TEXT", "NULL", nullable = true),
                    ColumnSpec("attachments", "TEXT", "NULL", nullable = true),
                    ColumnSpec("variantGroupId", "TEXT", "NULL", nullable = true),
                    ColumnSpec("variantIndex", "INTEGER NOT NULL", "1"),
                    ColumnSpec("tokenCount", "INTEGER NOT NULL", "0"),
                    ColumnSpec("thinkingTokens", "INTEGER NOT NULL", "0"),
                    ColumnSpec("responseTime", "INTEGER NOT NULL", "0"),
                    ColumnSpec("createdAt", "INTEGER NOT NULL", "0")
                ),
                indices = listOf("CREATE INDEX IF NOT EXISTS `index_messages_conversationId` ON `messages` (`conversationId`)")
            )
            repairTable(
                database,
                tableName = "api_usage_stats",
                columns = listOf(
                    ColumnSpec("id", "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL", "0"),
                    ColumnSpec("apiConfigId", "INTEGER NOT NULL", "0"),
                    ColumnSpec("provider", "TEXT NOT NULL", "'unknown'"),
                    ColumnSpec("modelName", "TEXT NOT NULL", "'unknown'"),
                    ColumnSpec("inputTokens", "INTEGER NOT NULL", "0"),
                    ColumnSpec("outputTokens", "INTEGER NOT NULL", "0"),
                    ColumnSpec("thinkingTokens", "INTEGER NOT NULL", "0"),
                    ColumnSpec("totalTokens", "INTEGER NOT NULL", "0"),
                    ColumnSpec("cachedTokens", "INTEGER NOT NULL", "0"),
                    ColumnSpec("responseTime", "INTEGER NOT NULL", "0"),
                    ColumnSpec("success", "INTEGER NOT NULL", "1"),
                    ColumnSpec("errorMessage", "TEXT", "NULL", nullable = true),
                    ColumnSpec("timestamp", "INTEGER NOT NULL", "0")
                )
            )
            repairTable(
                database,
                tableName = "environment_variables",
                columns = listOf(
                    ColumnSpec("id", "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL", "0"),
                    ColumnSpec("name", "TEXT NOT NULL", "''"),
                    ColumnSpec("value", "TEXT NOT NULL", "''"),
                    ColumnSpec("description", "TEXT", "NULL", nullable = true),
                    ColumnSpec("environment", "TEXT NOT NULL", "'default'"),
                    ColumnSpec("createdAt", "INTEGER NOT NULL", "0"),
                    ColumnSpec("updatedAt", "INTEGER NOT NULL", "0")
                )
            )
            repairTable(
                database,
                tableName = "prompt_templates",
                columns = listOf(
                    ColumnSpec("id", "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL", "0"),
                    ColumnSpec("name", "TEXT NOT NULL", "''"),
                    ColumnSpec("content", "TEXT NOT NULL", "''"),
                    ColumnSpec("description", "TEXT", "NULL", nullable = true),
                    ColumnSpec("category", "TEXT NOT NULL", "'general'"),
                    ColumnSpec("variables", "TEXT", "NULL", nullable = true),
                    ColumnSpec("isBuiltIn", "INTEGER NOT NULL", "0"),
                    ColumnSpec("useCount", "INTEGER NOT NULL", "0"),
                    ColumnSpec("createdAt", "INTEGER NOT NULL", "0"),
                    ColumnSpec("updatedAt", "INTEGER NOT NULL", "0")
                )
            )
            repairTable(
                database,
                tableName = "conversation_branches",
                columns = listOf(
                    ColumnSpec("id", "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL", "0"),
                    ColumnSpec("parentConversationId", "INTEGER NOT NULL", "0"),
                    ColumnSpec("branchMessageId", "INTEGER NOT NULL", "0"),
                    ColumnSpec("childConversationId", "INTEGER NOT NULL", "0"),
                    ColumnSpec("createdAt", "INTEGER NOT NULL", "0")
                ),
                indices = listOf("CREATE INDEX IF NOT EXISTS `index_conversation_branches_parentConversationId` ON `conversation_branches` (`parentConversationId`)")
            )
            repairTable(
                database,
                tableName = "selected_models",
                columns = listOf(
                    ColumnSpec("id", "INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL", "0"),
                    ColumnSpec("apiConfigId", "INTEGER NOT NULL", "0"),
                    ColumnSpec("modelName", "TEXT NOT NULL", "''"),
                    ColumnSpec("displayName", "TEXT", "NULL", nullable = true),
                    ColumnSpec("isEnabled", "INTEGER NOT NULL", "1"),
                    ColumnSpec("capability", "TEXT NOT NULL", "'auto'"),
                    ColumnSpec("sortOrder", "INTEGER NOT NULL", "0"),
                    ColumnSpec("createdAt", "INTEGER NOT NULL", "0")
                ),
                indices = listOf("CREATE INDEX IF NOT EXISTS `index_selected_models_apiConfigId` ON `selected_models` (`apiConfigId`)")
            )
        }

        private fun repairTable(
            database: SupportSQLiteDatabase,
            tableName: String,
            columns: List<ColumnSpec>,
            indices: List<String> = emptyList()
        ) {
            val createSql = buildCreateTableSql(tableName, columns)
            if (!tableExists(database, tableName)) {
                database.execSQL(createSql)
                indices.forEach { database.execSQL(it) }
                return
            }

            val tempTable = "${tableName}_room_repair"
            database.execSQL("DROP TABLE IF EXISTS `$tempTable`")
            database.execSQL(buildCreateTableSql(tempTable, columns))

            val existingColumns = getColumns(database, tableName)
            val columnNames = columns.joinToString(", ") { "`${it.name}`" }
            val selectValues = columns.joinToString(", ") { column ->
                if (existingColumns.contains(column.name)) {
                    if (column.nullable) {
                        "`${column.name}`"
                    } else {
                        "COALESCE(`${column.name}`, ${column.fallbackSql})"
                    }
                } else {
                    column.fallbackSql
                }
            }
            database.execSQL("INSERT INTO `$tempTable` ($columnNames) SELECT $selectValues FROM `$tableName`")
            database.execSQL("DROP TABLE `$tableName`")
            database.execSQL("ALTER TABLE `$tempTable` RENAME TO `$tableName`")
            indices.forEach { database.execSQL(it) }
        }

        private fun buildCreateTableSql(tableName: String, columns: List<ColumnSpec>): String {
            val definitions = columns.joinToString(", ") { "`${it.name}` ${it.definition}" }
            return "CREATE TABLE IF NOT EXISTS `$tableName` ($definitions)"
        }

        private fun tableExists(database: SupportSQLiteDatabase, tableName: String): Boolean {
            database.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(tableName)
            ).use { cursor ->
                return cursor.moveToFirst()
            }
        }

        private fun getColumns(database: SupportSQLiteDatabase, tableName: String): Set<String> {
            val columns = mutableSetOf<String>()
            database.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    columns += cursor.getString(nameIndex)
                }
            }
            return columns
        }

        private data class ColumnSpec(
            val name: String,
            val definition: String,
            val fallbackSql: String,
            val nullable: Boolean = false
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_assistant_database"
                )
                .addMigrations(
                    *LEGACY_REPAIR_MIGRATIONS
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
