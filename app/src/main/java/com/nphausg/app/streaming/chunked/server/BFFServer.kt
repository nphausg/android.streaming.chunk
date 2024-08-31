package com.nphausg.app.streaming.chunked.server

import android.os.Build
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.prepareGet
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.network.tls.CIOCipherSuites
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File


private const val MP3 = "bye_bye_bye_nsync.mp3"

object BFFServer {

    private const val PORT = 6868

    val url: String
        get() = String.format("%s:%d", NetworkUtils.getLocalIpAddress(), PORT)

    const val STREAMING = "/streaming"

    private val iODispatcher = Dispatchers.IO
    private val ioScope = CoroutineScope(iODispatcher)

    private val server by lazy {
        embeddedServer(Netty, PORT) {
            // configures Cross-Origin Resource Sharing. CORS is needed to make calls from arbitrary
            // JavaScript clients, and helps us prevent issues down the line.
            install(CORS) { anyHost() }
            install(DefaultHeaders)
            install(CallLogging)
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
            routing {
                //  staticResources
                staticResources("/static", "") {
                    default("index.html")
                }
                get("/") {
                    okText(call, "Hello!! You are here in ${Build.MODEL}")
                }
                get(STREAMING) {
                    val file = File("files/$MP3")
                    call.respondOutputStream(
                        contentType = ContentType.Audio.Any,
                        status = HttpStatusCode.OK
                    ) {
                        file.inputStream().use { inputStream ->
                            withContext(iODispatcher) {
                                inputStream.copyTo(this@respondOutputStream)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun okText(call: ApplicationCall, text: String) {
        call.respondText(
            text = text,
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json
        )
    }

    fun startStreaming() {
        ioScope.launch {
            val client = HttpClient(CIO) {
                engine {
                    // this: CIOEngineConfig
                    maxConnectionsCount = 1000
                    endpoint {
                        // this: EndpointConfig
                        maxConnectionsPerRoute = 100
                        pipelineMaxSize = 20
                        keepAliveTime = 5000
                        connectTimeout = 5000
                        connectAttempts = 5
                    }
//                    https {
//                        // this: TLSConfigBuilder
//                        serverName = "api.ktor.io"
//                        cipherSuites = CIOCipherSuites.SupportedSuites
//                    }
                }
            }
            val file = File.createTempFile("files", "index")
            runBlocking {
                client.prepareGet("https://ktor.io/").execute { httpResponse ->
                    val channel: ByteReadChannel = httpResponse.body()
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        while (!packet.isEmpty) {
                            val bytes = packet.readBytes()
                            println("Bytes saved: $bytes")
                            file.appendBytes(bytes)
                            println("Received ${file.length()} bytes from ${httpResponse.contentLength()}")
                        }
                    }
                    println("A file saved to ${file.path}")
                }
            }
        }
    }

    fun start() {
        ioScope.launch {
            try {
                server.start(wait = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        try {
            server.stop(1_000, 2_000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}