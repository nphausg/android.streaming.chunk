package com.nphausg.app.streaming.chunked.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.io.File

object Server {

    private const val HOST = "0.0.0.0"
    private const val PORT = 8080

    fun start() {
        embeddedServer(Netty, port = PORT, host = HOST) {
            install(ContentNegotiation) {
                json()
            }
            routing {
                get("/audio") {
                    val file = File("audio/sample.mp3")
                    if (file.exists()) {
                        call.respondFile(file)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "File not found")
                    }
                }
            }
        }.start(wait = true)
    }
}