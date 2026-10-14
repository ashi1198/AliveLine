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

/** v3 -> v4: tasks can be placed on the Daily Map (start time + duration). Both are optional. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `startMinute` INTEGER")
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `durationMin` INTEGER")
    }
}
/** v4 -> v5: tasks can have a reminder time. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderMinute` INTEGER")
    }
}
/** v5 -> v6: reminder style (notification or alarm) and note decorations (stickers and photos). */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderStyle` TEXT NOT NULL DEFAULT 'NOTIFICATION'")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `note_decorations` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`noteId` INTEGER NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`content` TEXT NOT NULL, " +
                    "`x` REAL NOT NULL, " +
                    "`y` REAL NOT NULL, " +
                    "`scale` REAL NOT NULL, " +
                    "`rotation` REAL NOT NULL, " +
                    "`z` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_decorations_noteId` ON `note_decorations` (`noteId`)")
    }
}