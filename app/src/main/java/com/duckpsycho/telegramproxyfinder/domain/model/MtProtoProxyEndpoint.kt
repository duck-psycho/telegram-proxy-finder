package com.duckpsycho.telegramproxyfinder.domain.model

import java.util.Locale

interface MtProtoProxyEndpoint {
    val server: String
    val port: Int
    val secret: String
}

fun MtProtoProxyEndpoint.identityKey(): String = "${server.trim().lowercase(Locale.US)}:$port:${secret.trim().lowercase(Locale.US)}"
