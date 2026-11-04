package br.com.laboratorio.android.live

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class BoundedAudioBufferTest {
    @Test
    fun bufferCheioDescartaChunkMaisAntigoEPreservaAudioRecente() {
        val buffer = BoundedAudioBuffer(capacity = 2, maxChunkBytes = 4)
        val first = byteArrayOf(1)
        val second = byteArrayOf(2)
        val third = byteArrayOf(3)

        assertTrue(buffer.offer(first))
        assertTrue(buffer.offer(second))
        assertTrue(buffer.offer(third))

        assertEquals(2, buffer.size)
        assertArrayEquals(second, buffer.poll(0, TimeUnit.MILLISECONDS))
        assertArrayEquals(third, buffer.poll(0, TimeUnit.MILLISECONDS))
        assertNull(buffer.poll(0, TimeUnit.MILLISECONDS))
    }

    @Test
    fun bufferRejeitaChunksVaziosOuAcimaDoLimite() {
        val buffer = BoundedAudioBuffer(capacity = 2, maxChunkBytes = 4)

        assertFalse(buffer.offer(byteArrayOf()))
        assertFalse(buffer.offer(byteArrayOf(1, 2, 3, 4, 5)))
        assertEquals(0, buffer.size)
    }

    @Test
    fun clearRemoveTodoAudioPendente() {
        val buffer = BoundedAudioBuffer(capacity = 2, maxChunkBytes = 4)
        buffer.offer(byteArrayOf(1))
        buffer.offer(byteArrayOf(2))

        buffer.clear()

        assertEquals(0, buffer.size)
        assertNull(buffer.poll(0, TimeUnit.MILLISECONDS))
    }
}
