package com.duckpsycho.telegramproxyfinder.data.source

object DefaultProxySourceUrls {
    val sources = listOf(
        ProxySource(
            SourceType.Web,
            "https://raw.githubusercontent.com/sakha1370/V2rayCollector/refs/heads/main/active_mtproto_proxies.txt",
        ),
        ProxySource(
            SourceType.Web,
            "https://raw.githubusercontent.com/kort0881/telegram-proxy-collector/refs/heads/main/proxy_all.txt",
        ),
        ProxySource(
            SourceType.Web,
            "https://raw.githubusercontent.com/devho3ein/tg-proxy/refs/heads/main/proxys/All_Proxys.txt",
        ),
        ProxySource(
            SourceType.Web,
            "https://raw.githubusercontent.com/Grim1313/mtproto-for-telegram/refs/heads/master/all_proxies.txt",
        ),
        ProxySource(
            SourceType.Telegram,
            "tgmtproxylol",
        ),
        ProxySource(
            SourceType.Telegram,
            "telemt_free_proxy",
        ),
        ProxySource(
            SourceType.Telegram,
            "TProxyRU",
        ),
    )
}
