package com.schoolos.attendance.core.hardware

import android.graphics.Bitmap

data class FaceEmbedding(
    val vector: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FaceEmbedding
        return vector.contentEquals(other.vector)
    }

    override fun hashCode(): Int {
        return vector.contentHashCode()
    }
}

data class RecognitionMatch(
    val personId: String,
    val identifier: String,
    val name: String,
    val confidence: Float
)

interface FaceRecognitionEngine {
    suspend fun extractEmbedding(faceBitmap: Bitmap): FaceEmbedding
    fun computeSimilarity(embeddingA: FaceEmbedding, embeddingB: FaceEmbedding): Float
    fun release()
}
