package com.aliveline.app.data.note

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "note_decorations",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE, // deleting a note removes its stickers and photos
        ),
    ],
    indices = [Index("noteId")],
)
data class NoteDecorationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val type: String,
    val content: String,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float,
    val z: Int,
)

data class NoteWithDecorations(
    @Embedded val note: NoteEntity,
    @Relation(parentColumn = "id", entityColumn = "noteId")
    val decorations: List<NoteDecorationEntity>,
)