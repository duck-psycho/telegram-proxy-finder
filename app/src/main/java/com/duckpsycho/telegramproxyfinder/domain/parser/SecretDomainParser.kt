package com.duckpsycho.telegramproxyfinder.domain.parser

import java.util.Locale

object SecretDomainParser {

    private const val EE_TOKEN_HEX_LENGTH = 32

    fun parse(secret: String): String? {
        val hex = secret.trim().lowercase(Locale.US)
        if (!hex.startsWith("ee")) return null
        return parseEeDomain(hex)
    }

    private fun parseEeDomain(hex: String): String? {
        val domainHexStart = 2 + EE_TOKEN_HEX_LENGTH
        if (hex.length <= domainHexStart) return null
        return decodeDomainHex(hex.substring(domainHexStart))
    }

    private fun decodeDomainHex(domainHex: String): String? {
        if (domainHex.isEmpty() || domainHex.length % 2 != 0) return null
        return domainHex.chunked(2)
            .mapNotNull { byte -> byte.toIntOrNull(16)?.toChar() }
            .joinToString("")
            .takeIf { domain -> domain.isNotEmpty() }
    }
}
