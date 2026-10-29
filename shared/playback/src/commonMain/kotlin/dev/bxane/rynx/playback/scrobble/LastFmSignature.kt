package dev.bxane.rynx.playback.scrobble

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

/** Last.fm requires MD5 for API request signatures; this checksum is not used for password storage. */
fun rynxMd5Hex(value: String): String = md5(value.encodeToByteArray()).joinToString("") { byte ->
    val unsigned = byte.toInt() and 0xff
    HEX[unsigned ushr 4].toString() + HEX[unsigned and 0x0f]
}

/** Last.fm's signature covers key/value pairs sorted by key, followed by the application secret. */
fun lastFmApiSignature(parameters: Map<String, String>, sharedSecret: String): String {
    require(sharedSecret.isNotEmpty()) { "Last.fm app credentials are not configured." }
    val source = buildString {
        parameters.toSortedMap().forEach { (key, value) ->
            if (key != "format" && key != "callback" && key != "api_sig") append(key).append(value)
        }
        append(sharedSecret)
    }
    return rynxMd5Hex(source)
}

private fun md5(input: ByteArray): ByteArray {
    val paddedSize = ((input.size + 9 + 63) / 64) * 64
    val padded = ByteArray(paddedSize)
    input.copyInto(padded)
    padded[input.size] = 0x80.toByte()
    val bitLength = input.size.toLong() * 8L
    for (index in 0 until 8) padded[paddedSize - 8 + index] = (bitLength ushr (index * 8)).toByte()

    var a0 = 0x67452301
    var b0 = 0xefcdab89.toInt()
    var c0 = 0x98badcfe.toInt()
    var d0 = 0x10325476
    val shifts = intArrayOf(
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
    )
    val constants = IntArray(64) { index -> floor(abs(sin(index + 1.0)) * 4_294_967_296.0).toLong().toInt() }
    val words = IntArray(16)
    for (offset in 0 until paddedSize step 64) {
        for (index in 0 until 16) {
            val start = offset + index * 4
            words[index] = (padded[start].toInt() and 0xff) or
                ((padded[start + 1].toInt() and 0xff) shl 8) or
                ((padded[start + 2].toInt() and 0xff) shl 16) or
                ((padded[start + 3].toInt() and 0xff) shl 24)
        }
        var a = a0
        var b = b0
        var c = c0
        var d = d0
        for (index in 0 until 64) {
            val function: Int
            val wordIndex: Int
            when (index) {
                in 0..15 -> { function = (b and c) or (b.inv() and d); wordIndex = index }
                in 16..31 -> { function = (d and b) or (d.inv() and c); wordIndex = (5 * index + 1) % 16 }
                in 32..47 -> { function = b xor c xor d; wordIndex = (3 * index + 5) % 16 }
                else -> { function = c xor (b or d.inv()); wordIndex = (7 * index) % 16 }
            }
            val sum = a + function + constants[index] + words[wordIndex]
            val rotated = (sum shl shifts[index]) or (sum ushr (32 - shifts[index]))
            a = d
            d = c
            c = b
            b += rotated
        }
        a0 += a
        b0 += b
        c0 += c
        d0 += d
    }
    val state = intArrayOf(a0, b0, c0, d0)
    return ByteArray(16) { index ->
        val word = state[index / 4]
        (word ushr ((index % 4) * 8)).toByte()
    }
}

private const val HEX = "0123456789abcdef"
