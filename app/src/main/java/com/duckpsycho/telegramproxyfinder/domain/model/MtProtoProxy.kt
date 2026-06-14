package com.duckpsycho.telegramproxyfinder.domain.model

data class MtProtoProxy(
    override val server: String,
    override val port: Int,
    override val secret: String,
) : MtProtoProxyEndpoint
