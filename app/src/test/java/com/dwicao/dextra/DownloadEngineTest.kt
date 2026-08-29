package com.dwicao.dextra

import com.dwicao.dextra.browser.BrowserClientIdentity
import com.dwicao.dextra.browser.DownloadEngine
import com.dwicao.dextra.data.DownloadEntry
import com.dwicao.dextra.data.DownloadStatus
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadEngineTest {
    @Test
    fun downloadsWithOneConsistentNativeRequest() = runBlocking {
        val payload = "Dextra download payload".toByteArray()
        val requestCount = AtomicInteger(0)
        val seenUserAgent = AtomicReference<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/file") { exchange ->
            requestCount.incrementAndGet()
            seenUserAgent.set(exchange.requestHeaders.getFirst("User-Agent"))
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        server.start()

        val output = Files.createTempFile("dextra-download", ".bin").toFile().apply { delete() }
        try {
            val updates = mutableListOf<String>()
            val engine = DownloadEngine(kotlinx.coroutines.CoroutineScope(Dispatchers.Unconfined)) { _, update ->
                updates += update.status
            }
            engine.execute(
                DownloadEntry(
                    downloadId = 1,
                    fileName = "file.bin",
                    url = "http://127.0.0.1:${server.address.port}/file",
                    mimeType = "application/octet-stream",
                    status = DownloadStatus.QUEUED.label,
                    bytesDownloaded = 0,
                    totalBytes = -1,
                    localUri = null,
                    filePath = output.path,
                    reason = null,
                    speedBytesPerSecond = 0,
                    createdAt = 0,
                ),
            )

            assertEquals(payload.toList(), output.readBytes().toList())
            assertEquals(1, requestCount.get())
            assertEquals(BrowserClientIdentity.userAgent, seenUserAgent.get())
            assertTrue(updates.contains(DownloadStatus.COMPLETE.label))
        } finally {
            output.delete()
            server.stop(0)
        }
    }

    @Test
    fun cancellationDoesNotReportCompletion() = runBlocking {
        val payload = "cancelled".toByteArray()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/file") { exchange ->
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        server.start()

        val output = Files.createTempFile("dextra-cancel", ".bin").toFile().apply { delete() }
        try {
            val updates = mutableListOf<String>()
            val engine = DownloadEngine(
                scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Unconfined),
                isCancelled = { true },
                onUpdate = { _, update -> updates += update.status },
            )
            engine.execute(
                DownloadEntry(
                    downloadId = 2,
                    fileName = "file.bin",
                    url = "http://127.0.0.1:${server.address.port}/file",
                    mimeType = "application/octet-stream",
                    status = DownloadStatus.QUEUED.label,
                    bytesDownloaded = 0,
                    totalBytes = -1,
                    localUri = null,
                    filePath = output.path,
                    reason = null,
                    speedBytesPerSecond = 0,
                    createdAt = 0,
                ),
            )

            assertTrue(DownloadStatus.PAUSED.label in updates)
            assertTrue(DownloadStatus.COMPLETE.label !in updates)
            assertTrue(!output.exists() || output.length() == 0L)
        } finally {
            output.delete()
            server.stop(0)
        }
    }
}
