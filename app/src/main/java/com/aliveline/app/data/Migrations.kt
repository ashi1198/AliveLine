package com.aliveline.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 -> v2: adds the notes table. Existing tasks are untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `notes` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`body` TEXT NOT NULL, " +
                    "`tag` TEXT, " +
                    "`pinned` INTEGER NOT NULL, " +
                    "`taskId` INTEGER, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_taskId` ON `notes` (`taskId`)")
    }
}

/** v2 -> v3: notes get a color. Existing notes keep the default look. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `notes` ADD COLUMN `color` TEXT NOT NULL DEFAULT 'DEFAULT'")
    }
}