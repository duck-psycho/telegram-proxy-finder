package com.duckpsycho.telegramproxyfinder.search

import android.util.Log
import com.duckpsycho.telegramproxyfinder.data.source.ProxySource
import com.duckpsycho.telegramproxyfinder.domain.ProxySourceLoader
import com.duckpsycho.telegramproxyfinder.domain.ProxyTester
import com.duckpsycho.telegramproxyfinder.domain.model.MtProtoProxy
import com.duckpsycho.telegramproxyfinder.domain.model.WorkingMtProtoProxy
import com.duckpsycho.telegramproxyfinder.domain.model.identityKey
import com.duckpsycho.telegramproxyfinder.domain.parser.MtProtoProxyParser
import com.duckpsycho.telegramproxyfinder.domain.parser.SecretDomainParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class ProxySearchService(
    private val sourceLoader: ProxySourceLoader,
    private val tester: ProxyTester,
    private val sources: List<ProxySource>,
    private val poolSize: Int = DEFAULT_POOL_SIZE,
) {

    fun search(): Flow<ProxySearchPhase> = channelFlow {
        var workersPrepared = 0
        try {
            send(ProxySearchPhase.LoadingSources)

            val rawLines = mutableSetOf<String>()
            val linesMutex = Mutex()
            val loadedCounter = AtomicInteger(0)
            val totalSources = sources.size

            coroutineScope {
                sources.map { source ->
                    async(Dispatchers.IO) {
                        val lines = loadSource(source)
                        val totalLines = linesMutex.withLock {
                            rawLines.addAll(lines)
                            rawLines.size
                        }
                        val loaded = loadedCounter.incrementAndGet()
                        Log.d(TAG, "Source $loaded/$totalSources: +${lines.size} lines, total=$totalLines")
                        send(ProxySearchPhase.SourcesProgress(loaded, totalSources))
                    }
                }.awaitAll()
            }

            send(ProxySearchPhase.Parsing)
            val validProxies = rawLines
                .mapNotNull(MtProtoProxyParser::parse)
                .distinctBy(MtProtoProxy::identityKey)

            if (validProxies.isEmpty()) {
                Log.w(TAG, "No valid proxies after filter (raw=${rawLines.size})")
                send(ProxySearchPhase.NoValidProxies)
                return@channelFlow
            }

            val totalToTest = validProxies.size
            val nextIndex = AtomicInteger(0)
            val checkedCounter = AtomicInteger(0)
            val foundProxies = mutableMapOf<String, WorkingMtProtoProxy>()
            val foundMutex = Mutex()

            send(ProxySearchPhase.Testing(0, totalToTest))

            val workers = minOf(poolSize, totalToTest).coerceAtLeast(1)
            withContext(Dispatchers.IO) {
                tester.prepareSearch()
                workersPrepared = workers
            }

            val testDispatcher = Dispatchers.IO.limitedParallelism(workers)
            coroutineScope {
                repeat(workers) {
                    launch(testDispatcher) {
                        while (true) {
                            val index = nextIndex.getAndIncrement()
                            if (index >= totalToTest) break

                            val proxy = validProxies[index]
                            val result = tester.test(proxy)
                            val checked = checkedCounter.incrementAndGet()
                            send(ProxySearchPhase.Testing(checked, totalToTest))

                            result.onSuccess { pingMs ->
                                val entry = WorkingMtProtoProxy(
                                    server = proxy.server.trim(),
                                    port = proxy.port,
                                    secret = proxy.secret.trim(),
                                    pingMs = pingMs,
                                    secretDomain = SecretDomainParser.parse(proxy.secret),
                                )
                                val shouldEmit = foundMutex.withLock {
                                    val key = entry.identityKey()
                                    val existing = foundProxies[key]
                                    if (existing == null || pingMs < existing.pingMs) {
                                        foundProxies[key] = entry
                                        true
                                    } else {
                                        false
                                    }
                                }
                                if (shouldEmit) {
                                    send(ProxySearchPhase.ProxyFound(entry))
                                }
                            }.onFailure { error ->
                                Log.e(TAG, "MTProto check failed: $proxy", error)
                            }
                        }
                    }
                }
            }

            send(ProxySearchPhase.Completed(foundProxies.size))
            Log.i(TAG, "Pipeline finished, tested=$totalToTest, pool=$workers, found=${foundProxies.size}")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Log.e(TAG, "Pipeline crashed", error)
            send(ProxySearchPhase.Failed(error.message.orEmpty()))
        } finally {
            if (workersPrepared > 0) {
                withContext(Dispatchers.IO) {
                    runCatching { tester.finishSearch() }
                        .onFailure { error -> Log.e(TAG, "Failed to shutdown TDLib worker pool", error) }
                }
            }
        }
    }

    private suspend fun loadSource(source: ProxySource): Set<String> =
        sourceLoader.load(source)

    companion object {
        private const val TAG = "ProxySearch"
        private const val DEFAULT_POOL_SIZE = 10
    }
}
