package com.nphausg.app.streaming.chunked.service

import java.io.File
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

object EncryptionService {

    private const val KEY_SIZE = 128
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"

    // Function to generate AES key
    fun generateAESKey(): Key {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE) // AES key size
        return keyGen.generateKey()
    }

    // Function to perform AES encryption
    fun encryptData(key: Key, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    // Function to perform AES decryption
    fun decryptData(key: Key, encryptedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(encryptedData)
    }

    // Function to simulate chunked streaming
    fun chunkedStream(data: ByteArray, chunkSize: Int): List<ByteArray> {
        val chunks = mutableListOf<ByteArray>()
        val inputStream = ByteArrayInputStream(data)

        while (inputStream.available() > 0) {
            val chunk = ByteArray(chunkSize)
            val bytesRead = inputStream.read(chunk)
            if (bytesRead > 0) {
                chunks.add(chunk.copyOf(bytesRead)) // Add only the bytes read
            }
        }

        return chunks
    }
}