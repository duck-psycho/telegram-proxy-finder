package com.duckpsycho.telegramproxyfinder.data.tdlib

import android.content.Context
import android.util.Log
import com.duckpsycho.telegramproxyfinder.BuildConfig
import com.duckpsycho.telegramproxyfinder.domain.model.MtProtoProxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

internal object TdLibProxyChecker {
    private const val TAG = "TdLibProxyChecker"
    private const val REQUEST_TIMEOUT_MS = 5_000L
    private const val CLIENT_POOL_SIZE = 10

    private val lifecycleMutex = Mutex()
    private val poolWriteLock = Any()
    private val nextClientIndex = AtomicInteger(0)

    @Volatile
    private var workRoot: File? = null

    @Volatile
    private var clientPool: List<Client> = emptyList()

    fun initialize(context: Context) {
        workRoot = File(context.filesDir, "tdlib-pings").apply { mkdirs() }
    }

    suspend fun prepareSearch() {
        val root = workRoot
            ?: error("Call TdLibProxyChecker.initialize(context) first")

        lifecycleMutex.withLock {
            val existing = clientPool
            if (existing.size >= CLIENT_POOL_SIZE) return

            if (existing.isEmpty()) {
                clearWorkRoot(root)
            }

            val created = coroutineScope {
                List(CLIENT_POOL_SIZE - existing.size) {
                    async { createClient(root) }
                }.awaitAll()
            }
            synchronized(poolWriteLock) {
                clientPool = clientPool + created
            }
            Log.i(TAG, "TDLib client pool ready, size=${clientPool.size}")
        }
    }

    suspend fun finishSearch() {
        clientPool.forEach { client ->
            val response = runCatching { client.sendAwait(TdApi.GetProxies()) }
                .onFailure { e -> Log.w(TAG, "getProxies failed during cleanup", e) }
                .getOrNull()
            val proxies = (response as? TdApi.Proxies)?.proxies ?: return@forEach
            proxies.forEach { proxy ->
                client.removeProxyAsync(proxy.id)
            }
        }
    }

    suspend fun testProxyWithPing(
        proxy: MtProtoProxy,
        timeoutSeconds: Double = 3.0,
    ): Result<Long> {
        val pool = clientPool
        if (pool.isEmpty()) {
            return Result.failure(IllegalStateException("TDLib client pool is not prepared, call prepareSearch() first"))
        }
        val client = pool[Math.floorMod(nextClientIndex.getAndIncrement(), pool.size)]
        val timeoutMs = (timeoutSeconds * 1_000).toLong()

        val addResponse = try {
            client.sendAwait(
                TdApi.AddProxy(proxy.server, proxy.port, false, TdApi.ProxyTypeMtproto(proxy.secret)),
            )
        } catch (error: TimeoutCancellationException) {
            return Result.failure(error)
        }
        val proxyId = when (addResponse) {
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

        return try {
            when (val pingResult = client.pingWithTimeout(proxyId, timeoutMs)) {
                is PingOutcome.Success -> Result.success(pingResult.pingMs)
                is PingOutcome.TimedOut -> Result.failure(ProxyPingTimeoutException(timeoutMs))
                is PingOutcome.Error -> Result.failure(pingResult.error)
                is PingOutcome.Unexpected -> Result.failure(pingResult.error)
            }
        } finally {
            client.removeProxyAsync(proxyId)
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

    private fun Client.removeProxyAsync(proxyId: Int) {
        send(TdApi.RemoveProxy(proxyId)) { result ->
            if (result is TdApi.Error) {
                Log.w(TAG, "removeProxy($proxyId) failed ${result.code}: ${result.message}")
            }
        }
    }

    private suspend fun createClient(root: File): Client {
        val tdReady = CompletableDeferred<Unit>()
        val clientDir = File(root, UUID.randomUUID().toString()).apply { mkdirs() }
        val clientRef = AtomicReference<Client?>(null)
        val earlyUpdates = Collections.synchronizedList(mutableListOf<TdApi.Object>())

        val client = Client.create(
            { update ->
                val c = clientRef.get()
                if (c == null) {
                    earlyUpdates.add(update)
                } else {
                    handleAuthorizationUpdate(
                        update = update,
                        client = c,
                        tdReady = tdReady,
                        clientDir = clientDir,
                    )
                }
            },
            { e -> Log.e(TAG, "TDLib update handler exception", e) },
            { e -> Log.e(TAG, "TDLib internal exception", e) },
        )
        clientRef.set(client)

        synchronized(earlyUpdates) {
            earlyUpdates.forEach { pending ->
                handleAuthorizationUpdate(
                    update = pending,
                    client = client,
                    tdReady = tdReady,
                    clientDir = clientDir,
                )
            }
            earlyUpdates.clear()
        }

        try {
            tdReady.await()
        } catch (error: Throwable) {
            client.send(TdApi.Close()) { }
            throw error
        }
        return client
    }

    private fun clearWorkRoot(root: File) {
        runCatching {
            if (root.exists()) {
                root.deleteRecursively()
            }
            root.mkdirs()
        }.onFailure { e ->
            Log.e(TAG, "clearWorkRoot failed", e)
        }
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

    private fun handleAuthorizationUpdate(
        update: TdApi.Object,
        client: Client,
        tdReady: CompletableDeferred<Unit>,
        clientDir: File,
    ) {
        if (update !is TdApi.UpdateAuthorizationState) return
        when (update.authorizationState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                if (tdReady.isCompleted) return

                val dbPath = File(clientDir, "tdlib-db").absolutePath
                val filesPath = File(clientDir, "tdlib-files").apply { mkdirs() }.absolutePath
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
                    applicationVersion = BuildConfig.VERSION_NAME
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
            is TdApi.AuthorizationStateClosed -> {
                Log.w(TAG, "TDLib client closed unexpectedly, removing from pool")
                tdReady.completeExceptionally(
                    IllegalStateException("TDLib client closed during initialization"),
                )
                synchronized(poolWriteLock) {
                    clientPool = clientPool.filter { it !== client }
                }
            }
        }
    }
}
