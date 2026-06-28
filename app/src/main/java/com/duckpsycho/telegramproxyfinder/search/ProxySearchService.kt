package com.duckpsycho.telegramproxyfinder.search

import android.util.Log
import com.duckpsycho.telegramproxyfinder.domain.ProxySourceLoader
import com.duckpsycho.telegramproxyfinder.domain.ProxyTester
import com.duckpsycho.telegramproxyfinder.domain.model.MtProtoProxy
import com.duckpsycho.telegramproxyfinder.domain.model.WorkingMtProtoProxy
import com.duckpsycho.telegramproxyfinder.domain.model.identityKey
import com.duckpsycho.telegramproxyfinder.domain.parser.MtProtoProxyParser
import com.duckpsycho.telegramproxyfinder.domain.parser.SecretDomainParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
    private val sourceUrls: List<String>,
    private val poolSize: Int = DEFAULT_POOL_SIZE,
) {

    fun search(): Flow<ProxySearchPhase> = channelFlow {
        var workersPrepared = 0
        try {
            send(ProxySearchPhase.LoadingSources)

            val rawLines = mutableSetOf<String>()
            sourceUrls.forEachIndexed { index, url ->
                val lines = sourceLoader.loadUrl(url)
                rawLines.addAll(lines)
                Log.d(TAG, "Source ${index + 1}/${sourceUrls.size}: +${lines.size} lines, total=${rawLines.size}")
                send(ProxySearchPhase.SourcesProgress(index + 1, sourceUrls.size))
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

            val proxiesToTest = validProxies.shuffled()
            val totalToTest = proxiesToTest.size
            val nextIndex = AtomicInteger(0)
            val checkedCounter = AtomicInteger(0)
            val foundProxies = mutableMapOf<String, WorkingMtProtoProxy>()
            val foundMutex = Mutex()

            send(ProxySearchPhase.Testing(0, totalToTest))

            val workers = minOf(poolSize, totalToTest).coerceAtLeast(1)
            withContext(Dispatchers.IO) {
                tester.prepareSearch(workers)
                workersPrepared = workers
            }

            val testDispatcher = Dispatchers.IO.limitedParallelism(workers)
            coroutineScope {
                repeat(workers) { workerSlot ->
                    launch(testDispatcher) {
                        while (true) {
                            val index = nextIndex.getAndIncrement()
                            if (index >= totalToTest) break

                            val proxy = proxiesToTest[index]
                            val result = tester.test(proxy, workerSlot)
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

    companion object {
        private const val TAG = "ProxySearch"
        private const val DEFAULT_POOL_SIZE = 10
    }
}
