package com.duckpsycho.telegramproxyfinder.data.tdlib

import android.content.Context
import android.util.Log
import com.duckpsycho.telegramproxyfinder.domain.model.MtProtoProxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal object TdLibProxyChecker {
    private const val TAG = "TdLibProxyChecker"
    private const val REQUEST_TIMEOUT_MS = 5_000L
    private const val REMOVE_PROXY_TIMEOUT_MS = 2_000L
    private const val ABANDON_CLOSE_TIMEOUT_MS = 2_000L

    private val lifecycleMutex = Mutex()
    private val abandonScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var pingWorkRoot: File? = null

    @Volatile
    private var workerPool: List<WorkerSlot>? = null

    private class WorkerSlot(
        val client: Client,
        val slotDir: File,
        val requestMutex: Mutex = Mutex(),
    )

    fun initialize(context: Context) {
        pingWorkRoot = File(context.filesDir, "tdlib-pings").apply { mkdirs() }
    }

    suspend fun prepareWorkerPool(workerCount: Int) {
        val root = pingWorkRoot
            ?: error("Call TdLibProxyChecker.initialize(context) first")

        val previousPool = detachWorkerPool()
        closeWorkerPool(previousPool)
        lifecycleMutex.withLock {
            clearPingWorkDirectoryLocked(root)
        }

        val slots = coroutineScope {
            List(workerCount) {
                async { createWorkerSlot(root) }
            }.awaitAll()
        }

        lifecycleMutex.withLock {
            workerPool = slots
            Log.i(TAG, "Worker pool ready, size=$workerCount")
        }
    }

    suspend fun shutdownWorkerPool() {
        closeWorkerPool(detachWorkerPool())
    }

    private suspend fun detachWorkerPool(): List<WorkerSlot>? = lifecycleMutex.withLock {
        val pool = workerPool
        workerPool = null
        pool
    }

    suspend fun testProxyWithPing(
        proxy: MtProtoProxy,
        workerSlot: Int,
        timeoutSeconds: Double = 3.0,
    ): Result<Long> {
        val slot = workerPool?.getOrNull(workerSlot)
            ?: return Result.failure(IllegalStateException("Worker pool is not prepared for slot $workerSlot"))

        return slot.requestMutex.withLock {
            testProxyWithPingLocked(
                slot = slot,
                workerSlot = workerSlot,
                proxy = proxy,
                timeoutMs = (timeoutSeconds * 1_000).toLong(),
            )
        }
    }

    private suspend fun testProxyWithPingLocked(
        slot: WorkerSlot,
        workerSlot: Int,
        proxy: MtProtoProxy,
        timeoutMs: Long,
    ): Result<Long> {
        val client = slot.client
        var proxyId = -1
        var abandonSlot = false

        return try {
            val addResponse = client.sendAwait(
                TdApi.AddProxy(proxy.server, proxy.port, false, TdApi.ProxyTypeMtproto(proxy.secret)),
                timeoutMs = REQUEST_TIMEOUT_MS,
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

            when (val pingResult = client.pingWithTimeout(proxyId, timeoutMs)) {
                is PingOutcome.Success -> Result.success(pingResult.pingMs)
                is PingOutcome.TimedOut -> {
                    abandonSlot = true
                    proxyId = -1
                    Result.failure(ProxyPingTimeoutException(timeoutMs))
                }
                is PingOutcome.Error -> Result.failure(pingResult.error)
                is PingOutcome.Unexpected -> Result.failure(pingResult.error)
            }
        } catch (error: TimeoutCancellationException) {
            abandonSlot = true
            Result.failure(error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        } finally {
            if (proxyId >= 0) {
                val removed = withContext(NonCancellable) {
                    runCatching {
                        client.sendAwait(
                            TdApi.RemoveProxy(proxyId),
                            timeoutMs = REMOVE_PROXY_TIMEOUT_MS,
                        )
                    }.onFailure { e ->
                        Log.w(TAG, "removeProxy failed for id=$proxyId", e)
                    }.isSuccess
                }
                if (!removed) {
                    abandonSlot = true
                }
            }
            if (abandonSlot) {
                resetWorkerSlot(workerSlot, slot)
            }
        }
    }

    private class ProxyPingTimeoutException(timeoutMs: Long) :
        Exception("Timed out waiting for $timeoutMs ms")

    private sealed interface PingOutcome {
        data class Success(val pingMs: Long) : PingOutcome
        data object TimedOut : PingOutcome
        data class Error(val error: IllegalStateException) : PingOutcome
        data class Unexpected(val error: IllegalStateException) : PingOutcome
    }

    private suspend fun Client.pingWithTimeout(proxyId: Int, timeoutMs: Long): PingOutcome {
        val responseDeferred = CompletableDeferred<TdApi.Object>()
        send(TdApi.PingProxy(proxyId)) { response ->
            responseDeferred.complete(response)
        }

        val pingResponse = try {
            withTimeout(timeoutMs) {
                responseDeferred.await()
            }
        } catch (_: TimeoutCancellationException) {
            return PingOutcome.TimedOut
        }

        return when (pingResponse) {
            is TdApi.Seconds -> {
                val pingMs = (pingResponse.seconds * 1_000.0).toLong().coerceAtLeast(1L)
                PingOutcome.Success(pingMs)
            }
            is TdApi.Error -> PingOutcome.Error(
                IllegalStateException("pingProxy failed ${pingResponse.code}: ${pingResponse.message}"),
            )
            else -> PingOutcome.Unexpected(
                IllegalStateException("Unexpected TDLib response: ${pingResponse.javaClass.simpleName}"),
            )
        }
    }

    private suspend fun resetWorkerSlot(workerSlot: Int, oldSlot: WorkerSlot) {
        val root = pingWorkRoot ?: return
        Log.w(TAG, "Resetting worker slot $workerSlot after stuck TDLib request")

        val newSlot = createWorkerSlot(root)
        lifecycleMutex.withLock {
            val pool = workerPool
            if (pool == null || pool.getOrNull(workerSlot) !== oldSlot) {
                abandonWorkerSlot(newSlot)
                return
            }
            workerPool = pool.toMutableList().apply { this[workerSlot] = newSlot }
        }
        abandonWorkerSlot(oldSlot)
    }

    private fun abandonWorkerSlot(slot: WorkerSlot) {
        abandonScope.launch {
            withTimeoutOrNull(ABANDON_CLOSE_TIMEOUT_MS) {
                slot.client.closeAwait()
            }
            runCatching { slot.slotDir.deleteRecursively() }
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

    private suspend fun closeWorkerPool(pool: List<WorkerSlot>?) {
        pool?.forEach { slot ->
            slot.requestMutex.withLock {
                runCatching {
                    withTimeout(ABANDON_CLOSE_TIMEOUT_MS) {
                        slot.client.closeAwait()
                    }
                }.onFailure { e ->
                    Log.w(TAG, "TDLib close failed for ${slot.slotDir.name}", e)
                }
            }
            runCatching { slot.slotDir.deleteRecursively() }
        }
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
        val closed = CompletableDeferred<Unit>()
        send(TdApi.Close()) { _ ->
            closed.complete(Unit)
        }
        closed.await()
    }

    private suspend fun Client.sendAwait(
        query: TdApi.Function<*>,
        timeoutMs: Long = REQUEST_TIMEOUT_MS,
    ): TdApi.Object {
        val result = CompletableDeferred<TdApi.Object>()
        send(query) { response ->
            result.complete(response)
        }
        return withTimeout(timeoutMs) {
            result.await()
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
