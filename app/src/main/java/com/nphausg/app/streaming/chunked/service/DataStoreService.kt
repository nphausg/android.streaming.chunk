package com.nphausg.app.streaming.chunked.service

import java.io.File

object DataStoreService {

    // Function to read audio file into byte array
    fun readAudioFile(filePath: String): ByteArray {
        return File(filePath).readBytes()
    }

    // Function to write byte array to audio file
    fun writeAudioFile(data: ByteArray, filePath: String) {
        File(filePath).writeBytes(data)
    }

}