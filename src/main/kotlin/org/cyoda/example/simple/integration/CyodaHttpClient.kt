package org.cyoda.example.simple.integration

import io.ktor.client.*
import org.cyoda.example.simple.config.ClientConnectionProperties

data class CyodaHttpClient(
    val ktorClient: HttpClient,
    private val tokenManager: TokenManager,
    val connectionProperties: ClientConnectionProperties
) {
    fun getAccessTokenBlocking() =
        tokenManager.getAccessTokenBlocking(connectionProperties.refreshUrl)

}