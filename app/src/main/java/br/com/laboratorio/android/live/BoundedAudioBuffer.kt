package br.com.laboratorio.android.live

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Buffer de áudio limitado em quantidade e tamanho de chunk.
 * Quando a reprodução fica atrasada, descarta o chunk mais antigo para priorizar áudio recente.
 */
internal class BoundedAudioBuffer(
    capacity: Int = DEFAULT_CAPACITY,
    private val maxChunkBytes: Int = DEFAULT_MAX_CHUNK_BYTES,
) {
    private val chunks = ArrayBlockingQueue<ByteArray>(capacity)

    init {
        require(capacity > 0) { "A capacidade precisa ser positiva." }
        require(maxChunkBytes > 0) { "O tamanho máximo do chunk precisa ser positivo." }
    }

    @Synchronized
    fun offer(chunk: ByteArray): Boolean {
        if (chunk.isEmpty() || chunk.size > maxChunkBytes) return false
        if (chunks.offer(chunk)) return true

        // Não acumula latência: remove o áudio mais antigo e tenta inserir o mais recente.
        chunks.poll()
        return chunks.offer(chunk)
    }

    @Throws(InterruptedException::class)
    fun poll(timeout: Long, unit: TimeUnit): ByteArray? = chunks.poll(timeout, unit)

    fun clear() = chunks.clear()

    val size: Int
        get() = chunks.size

    companion object {
        const val DEFAULT_CAPACITY = 32
        const val DEFAULT_MAX_CHUNK_BYTES = 64 * 1024
    }
}
