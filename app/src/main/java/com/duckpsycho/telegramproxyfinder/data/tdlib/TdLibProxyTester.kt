package com.duckpsycho.telegramproxyfinder.data.tdlib

import android.content.Context
import com.duckpsycho.telegramproxyfinder.domain.ProxyTester
import com.duckpsycho.telegramproxyfinder.domain.model.MtProtoProxy

class TdLibProxyTester(
    context: Context,
) : ProxyTester {
    init {
        TdLibProxyChecker.initialize(context.applicationContext)
    }

    override suspend fun prepareSearch() {
        TdLibProxyChecker.prepareSearch()
    }

    override suspend fun test(proxy: MtProtoProxy): Result<Long> = TdLibProxyChecker.testProxyWithPing(proxy)

    override suspend fun finishSearch() {
        TdLibProxyChecker.finishSearch()
    }
}
