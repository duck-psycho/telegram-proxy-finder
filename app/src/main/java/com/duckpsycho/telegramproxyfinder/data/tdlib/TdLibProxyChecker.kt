package com.duckpsycho.telegramproxyfinder.data.tdlib

import android.content.Context
import android.util.Log
import com.duckpsycho.telegramproxyfinder.domain.model.MtProtoProxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

internal object TdLibProxyChecker {
    private const val TAG = "TdLibProxyChecker"

    private val lifecycleMutex = Mutex()

    @Volatile
    private var pingWorkRoot: File? = null

    @Volatile
    private var workerPool: List<WorkerSlot>? = null

    private class WorkerSlot(
        val client: Client,
        val slotDir: File,
    )

    fun initialize(context: Context) {
        pingWorkRoot = File(context.filesDir, "tdlib-pings").apply { mkdirs() }
    }

    suspend fun prepareWorkerPool(workerCount: Int) {
        val root = pingWorkRoot
            ?: error("Call TdLibProxyChecker.initialize(context) first")

        lifecycleMutex.withLock {
            shutdownWorkerPoolLocked()
            clearPingWorkDirectoryLocked(root)
            workerPool = List(workerCount) { createWorkerSlot(root) }
            Log.i(TAG, "Worker pool ready, size=$workerCount")
        }
    }

    suspend fun shutdownWorkerPool() {
        lifecycleMutex.withLock {
            shutdownWorkerPoolLocked()
        }
    }

    suspend fun testProxyWithPing(
        proxy: MtProtoProxy,
        workerSlot: Int,
        timeoutSeconds: Double = 3.0,
    ): Result<Long> {
        val slot = workerPool?.getOrNull(workerSlot)
            ?: return Result.failure(IllegalStateException("Worker pool is not prepared for slot $workerSlot"))

        val client = slot.client
        var proxyId = -1

        return try {
            val addResponse = client.sendAwait(
                TdApi.AddProxy(proxy.server, proxy.port, false, TdApi.ProxyTypeMtproto(proxy.secret)),
            )
            proxyId = when (addResponse) {
                is TdApi.Proxy -> addResponse.id
                is TdApi.Error ->
                    return Result.failure(
                        IllegalStateException("addProxy failed ${addResponse.code}: ${addResponse.message}"),
                    )
                else ->
                    return Result.failure(
                        IllegalStateException("Unexpected TDLib response: ${addResponse.javaClass.simpleName}"),
                    )
            }

            val pingResponse = withTimeout((timeoutSeconds * 1_000).toLong()) {
                client.sendAwait(TdApi.PingProxy(proxyId))
            }
            when (pingResponse) {
                is TdApi.Seconds -> {
                    val pingMs = (pingResponse.seconds * 1_000.0).toLong().coerceAtLeast(1L)
                    Result.success(pingMs)
                }
                is TdApi.Error ->
                    Result.failure(
                        IllegalStateException("pingProxy failed ${pingResponse.code}: ${pingResponse.message}"),
                    )
                else ->
                    Result.failure(
                        IllegalStateException("Unexpected TDLib response: ${pingResponse.javaClass.simpleName}"),
                    )
            }
        } catch (error: TimeoutCancellationException) {
            Result.failure(error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        } finally {
            if (proxyId >= 0) {
                runCatching { client.sendAwait(TdApi.RemoveProxy(proxyId)) }
            }
        }
    }

    private suspend fun createWorkerSlot(root: File): WorkerSlot {
        val tdReady = CompletableDeferred<Unit>()
        val slotDir = File(root, UUID.randomUUID().toString()).apply { mkdirs() }
        val clientRef = AtomicReference<Client?>(null)
        val earlyAuthUpdates = Collections.synchronizedList(mutableListOf<TdApi.Object>())

        val client = Client.create(
            { update ->
                val c = clientRef.get()
                if (c == null) {
                    earlyAuthUpdates.add(update)
                } else {
                    handleEphemeralAuthorization(
                        update = update,
                        client = c,
                        tdReady = tdReady,
                        slotDir = slotDir,
                    )
                }
            },
            { e -> Log.e(TAG, "TDLib update handler exception", e) },
            { e -> Log.e(TAG, "TDLib internal exception", e) },
        )
        clientRef.set(client)

        synchronized(earlyAuthUpdates) {
            earlyAuthUpdates.forEach { pending ->
                handleEphemeralAuthorization(
                    update = pending,
                    client = client,
                    tdReady = tdReady,
                    slotDir = slotDir,
                )
            }
            earlyAuthUpdates.clear()
        }

        tdReady.await()
        return WorkerSlot(client = client, slotDir = slotDir)
    }

    private suspend fun shutdownWorkerPoolLocked() {
        workerPool?.forEach { slot ->
            runCatching { slot.client.closeAwait() }
            runCatching { slot.slotDir.deleteRecursively() }
        }
        workerPool = null
    }

    private fun clearPingWorkDirectoryLocked(root: File) {
        runCatching {
            if (root.exists()) {
                root.deleteRecursively()
            }
            root.mkdirs()
        }.onFailure { e ->
            Log.e(TAG, "clearPingWorkDirectory failed", e)
        }
    }

    private suspend fun Client.closeAwait() {
        suspendCancellableCoroutine { continuation ->
            send(TdApi.Close()) { _ ->
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    private suspend fun Client.sendAwait(query: TdApi.Function<*>): TdApi.Object =
        suspendCancellableCoroutine { continuation ->
            send(query) { response ->
                if (continuation.isActive) {
                    continuation.resume(response)
                }
            }
        }

    private fun handleEphemeralAuthorization(
        update: TdApi.Object,
        client: Client,
        tdReady: CompletableDeferred<Unit>,
        slotDir: File,
    ) {
        if (update !is TdApi.UpdateAuthorizationState) return
        when (update.authorizationState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                if (tdReady.isCompleted) return

                val dbPath = File(slotDir, "tdlib-db").absolutePath
                val filesPath = File(slotDir, "tdlib-files").apply { mkdirs() }.absolutePath
                val params = TdApi.SetTdlibParameters().apply {
                    useTestDc = false
                    databaseDirectory = dbPath
                    filesDirectory = filesPath
                    databaseEncryptionKey = byteArrayOf()
                    useFileDatabase = false
                    useChatInfoDatabase = false
                    useMessageDatabase = false
                    useSecretChats = false
                    apiId = 94575
                    apiHash = "a3406de8d171bb422bb6ddf3bbd800e2"
                    systemLanguageCode = "en"
                    deviceModel = "Android"
                    systemVersion = "Android"
                    applicationVersion = "1.0"
                }
                client.send(params) { result ->
                    when (result) {
                        is TdApi.Ok -> tdReady.complete(Unit)
                        is TdApi.Error -> tdReady.completeExceptionally(
                            IllegalStateException("setTdlibParameters failed ${result.code}: ${result.message}"),
                        )
                        else -> tdReady.completeExceptionally(
                            IllegalStateException("Unexpected response: ${result.javaClass.simpleName}"),
                        )
                    }
                }
            }
        }
    }
}
