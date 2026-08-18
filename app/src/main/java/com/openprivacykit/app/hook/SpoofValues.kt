package com.openprivacykit.app.hook

import java.security.SecureRandom

/**
 * Generates stable-but-fake identifier values.
 * A random value is seeded per (app, field) so it stays constant across
 * app restarts within the same boot, but differs between apps — this
 * avoids breaking apps that expect a stable ANDROID_ID while still
 * defeating cross-app correlation.
 */
object SpoofValues {
    private val random = SecureRandom()

    fun androidId(seed: String): String {
        val r = java.util.Random(seed.hashCode().toLong() xor System.nanoTime())
        return r.nextLong().toString(16).padStart(16, '0').take(16)
    }

    fun macAddress(): String {
        val bytes = ByteArray(6)
        random.nextBytes(bytes)
        // locally administered, unicast
        bytes[0] = ((bytes[0].toInt() and 0xFE) or 0x02).toByte()
        return bytes.joinToString(":") { String.format("%02X", it) }
    }

    fun hexId(len: Int): String {
        val chars = "0123456789abcdef"
        return (1..len).map { chars[random.nextInt(16)] }.joinToString("")
    }
}
