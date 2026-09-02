package com.ai_assistant.studentfocus.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcard_decks")
data class DeckEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val deckId: String,
    val front: String,
    val back: String,
    val difficulty: String = "Good", // "Hard", "Good", "Easy", "Unreviewed"
    val interval: Int = 1,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val nextDueDate: Long = System.currentTimeMillis()
)

