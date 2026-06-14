package com.duckpsycho.telegramproxyfinder.domain.model

data class WorkingMtProtoProxy(
    override val server: String,
    override val port: Int,
    override val secret: String,
    val pingMs: Long,
) : MtProtoProxyEndpoint
