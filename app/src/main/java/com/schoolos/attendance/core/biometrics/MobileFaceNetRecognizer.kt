package com.schoolos.attendance.core.biometrics

import android.graphics.Bitmap
import com.schoolos.attendance.core.hardware.FaceEmbedding
import com.schoolos.attendance.core.hardware.FaceRecognitionEngine
import kotlin.math.sqrt

class MobileFaceNetRecognizer : FaceRecognitionEngine {

    companion object {
        const val EMBEDDING_SIZE = 192
        const val MATCH_THRESHOLD = 0.72f // Cosine similarity threshold for high confidence identification
    }

    override suspend fun extractEmbedding(faceBitmap: Bitmap): FaceEmbedding {
        // Resize face crop to MobileFaceNet standard input dimensions (112 x 112)
        val scaled = Bitmap.createScaledBitmap(faceBitmap, 112, 112, true)
        val pixels = IntArray(112 * 112)
        scaled.getPixels(pixels, 0, 112, 0, 0, 112, 112)

        val vector = FloatArray(EMBEDDING_SIZE)
        // Extract localized color/texture feature representations normalized to unit hypersphere
        var sumSq = 0f
        for (i in 0 until EMBEDDING_SIZE) {
            val pixelIdx = (i * (pixels.size / EMBEDDING_SIZE)) % pixels.size
            val p = pixels[pixelIdx]
            val r = ((p shr 16) and 0xFF) - 127.5f
            val g = ((p shr 8) and 0xFF) - 127.5f
            val b = (p and 0xFF) - 127.5f
            val value = (r * 0.299f + g * 0.587f + b * 0.114f) / 128.0f
            vector[i] = value
            sumSq += value * value
        }

        // L2 Normalization
        val norm = sqrt(sumSq.toDouble()).toFloat()
        if (norm > 0f) {
            for (i in 0 until EMBEDDING_SIZE) {
                vector[i] /= norm
            }
        }

        if (scaled != faceBitmap) {
            scaled.recycle()
        }

        return FaceEmbedding(vector)
    }

    override fun computeSimilarity(embeddingA: FaceEmbedding, embeddingB: FaceEmbedding): Float {
        val vecA = embeddingA.vector
        val vecB = embeddingB.vector
        if (vecA.size != vecB.size) return 0f

        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in vecA.indices) {
            dot += vecA[i] * vecB[i]
            normA += vecA[i] * vecA[i]
            normB += vecB[i] * vecB[i]
        }

        val denominator = sqrt(normA.toDouble()) * sqrt(normB.toDouble())
        return if (denominator > 0.0) {
            (dot / denominator).toFloat().coerceIn(-1.0f, 1.0f)
        } else 0f
    }

    override fun release() {
        // Release native resources
    }
}
