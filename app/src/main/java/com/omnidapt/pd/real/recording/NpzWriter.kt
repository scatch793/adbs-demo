package com.omnidapt.pd.real.recording

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object NpzWriter {
    fun twoChannelInt16(samples: ShortArray, sampleRateHz: Int): ByteArray {
        require(samples.size % 2 == 0)
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("samples.npy"))
            zip.write(npy("<i2", "(${samples.size / 2}, 2)", samples.size * 2) { buffer ->
                samples.forEach(buffer::putShort)
            })
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("sample_rate_hz.npy"))
            zip.write(npy("<i4", "()", 4) { it.putInt(sampleRateHz) })
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private fun npy(
        descriptor: String,
        shape: String,
        payloadBytes: Int,
        writer: (ByteBuffer) -> Unit,
    ): ByteArray {
        val prefixSize = 10
        var header = "{'descr': '$descriptor', 'fortran_order': False, 'shape': $shape, }"
        val padding = (16 - ((prefixSize + header.length + 1) % 16)) % 16
        header += " ".repeat(padding) + "\n"
        val headerBytes = header.encodeToByteArray()
        val output = ByteBuffer.allocate(prefixSize + headerBytes.size + payloadBytes)
            .order(ByteOrder.LITTLE_ENDIAN)
        output.put(byteArrayOf(0x93.toByte(), 'N'.code.toByte(), 'U'.code.toByte(), 'M'.code.toByte(), 'P'.code.toByte(), 'Y'.code.toByte()))
        output.put(1)
        output.put(0)
        output.putShort(headerBytes.size.toShort())
        output.put(headerBytes)
        writer(output)
        return output.array()
    }
}
