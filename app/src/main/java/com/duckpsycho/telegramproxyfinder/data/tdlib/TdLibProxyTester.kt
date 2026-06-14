package com.duckpsycho.telegramproxyfinder.data.tdlib

import android.content.Context
import com.duckpsycho.telegramproxyfinder.domain.ProxyTester
import com.duckpsycho.telegramproxyfinder.domain.model.MtProtoProxy

class TdLibProxyTester(context: Context) : ProxyTester {

    init {
        TdLibProxyChecker.initialize(context.applicationContext)
    }

    override suspend fun prepareSearch(workerCount: Int) {
        TdLibProxyChecker.prepareWorkerPool(workerCount)
    }

    override suspend fun test(proxy: MtProtoProxy, workerSlot: Int): Result<Long> =
        TdLibProxyChecker.testProxyWithPing(proxy, workerSlot)

    override suspend fun finishSearch() {
        TdLibProxyChecker.shutdownWorkerPool()
    }
}
