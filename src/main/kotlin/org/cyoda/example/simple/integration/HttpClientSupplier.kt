package org.cyoda.example.simple.integration

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import org.cyoda.example.simple.config.ClientConnectionProperties

class HttpClientSupplier(private val connectionProperties: ClientConnectionProperties) {

    val loginUrl = connectionProperties.loginUrl
    val refreshUrl = connectionProperties.refreshUrl

    suspend fun connect(tokenClient: TokenManager, username: String, password: String): CyodaHttpClient {
        val loginClient = loginClient()
        val client = loginClient.post(loginUrl) {
            headers {
                headers.append("X-Requested-With", "XMLHttpRequest")
            }
            contentType(ContentType.Application.Json)
            setBody(LoginBody(username, password))
        }.body<Map<String, String>>()
            .let { BearerTokens(it["token"]!!, it["refreshToken"]!!) }
            .also { tokenClient.refreshToken = it.refreshToken }
            .newClient(tokenClient)
        return client
    }

    private fun loginClient(requestTimeout: Long = 60_000L) = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = requestTimeout
        }
        install(HttpRequestRetry)
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE
        }
        install(ContentNegotiation) {
            json()
        }
        expectSuccess = true
    }

    private suspend fun BearerTokens.newClient(tokenManager:TokenManager, requestTimeout: Long = 60_000L): CyodaHttpClient = this
        .let { tokens ->
            HttpClient(CIO) {
                install(Auth) {
                    bearer {
                        loadTokens {
                            tokens
                        }
                        refreshTokens {
                            // Refresh the tokens using the token manager
                            tokenManager.refreshToken = refreshToken
                            val accessToken = tokenManager.getAccessToken(refreshUrl)
                            BearerTokens(accessToken, refreshToken)
                        }
                    }
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = requestTimeout
                    connectTimeoutMillis = 30_000
                }
                install(HttpRequestRetry)
                install(Logging) {
                    logger = Logger.SIMPLE
                    level = LogLevel.NONE
                }
                install(ContentNegotiation) {
                    json()
                }
                expectSuccess = false
            }.let {
                CyodaHttpClient(it, tokenManager,connectionProperties)
            }
        }





    @Serializable
    data class LoginBody(val username: String, val password: String)

    @Serializable
    data class TokenStorage(var token: String, var refreshToken: String)
}