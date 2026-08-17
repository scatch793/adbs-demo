package com.omnidapt.pd.real.recording

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class NpzWriterTest {
    @Test
    fun createsNumpyCompatibleArchiveStructure() {
        val bytes = NpzWriter.twoChannelInt16(shortArrayOf(1, -2, 3, -4), 256)
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }
        assertEquals(setOf("samples.npy", "sample_rate_hz.npy"), entries.keys)
        assertArrayEquals(
            byteArrayOf(0x93.toByte(), 'N'.code.toByte(), 'U'.code.toByte(), 'M'.code.toByte(), 'P'.code.toByte(), 'Y'.code.toByte()),
            entries.getValue("samples.npy").copyOfRange(0, 6),
        )
    }
}
