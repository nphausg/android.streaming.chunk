package com.nphausg.app.streaming.chunked.service

import java.io.ByteArrayOutputStream

object StreamingService {

    private const val ENCRYPTED_FILE_PATH = "encrypted_audio.dat"
    private const val DECRYPTED_FILE_PATH = "decrypted_audio.wav"

    // Main function to test encryption, chunking, and decryption with audio data
    fun stream(inputBytes: ByteArray) {

        // Read audio file
        println("Original Audio Data Size: ${inputBytes.size} bytes")

        // Generate AES key
        val aesKey = EncryptionService.generateAESKey()

        // Encrypt audio data
        val encryptedAudioData = EncryptionService.encryptData(aesKey, inputBytes)
        println("Encrypted Audio Data Size: ${encryptedAudioData.size} bytes")

        // Write encrypted data to file
        DataStoreService.writeAudioFile(encryptedAudioData, ENCRYPTED_FILE_PATH)
        println("Encrypted audio written to $ENCRYPTED_FILE_PATH")

        // Chunk encrypted data
        val chunkSize = 1024 // Chunk size in bytes
        val chunks = EncryptionService.chunkedStream(encryptedAudioData, chunkSize)
        println("Chunked Encrypted Data: ${chunks.size} chunks")

        // Decrypt each chunk and reconstruct original audio data
        val decryptedStream = ByteArrayOutputStream()
        chunks.forEach { chunk ->
            val decryptedChunk = EncryptionService.decryptData(aesKey, chunk)
            decryptedStream.write(decryptedChunk)
        }

        val decryptedAudioData = decryptedStream.toByteArray()
        println("Decrypted Audio Data Size: ${decryptedAudioData.size} bytes")

        // Write decrypted data to output audio file
        DataStoreService.writeAudioFile(decryptedAudioData, DECRYPTED_FILE_PATH)
        println("Decrypted audio written to $DECRYPTED_FILE_PATH")
    }
}