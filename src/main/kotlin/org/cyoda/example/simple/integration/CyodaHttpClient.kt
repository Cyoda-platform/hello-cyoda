package org.cyoda.example.simple.integration

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.cyoda.example.simple.config.ClientConnectionProperties

class CyodaHttpClient(
    val ktorClient: HttpClient,
    private val tokenManager: TokenManager,
    val connectionProperties: ClientConnectionProperties
) {

    fun getAccessTokenBlocking() =
        tokenManager.getAccessTokenBlocking(connectionProperties.refreshUrl)

    suspend fun publish(
        obj: Any,
        model: String,
        modelVersion: Int,
        transactionWindow: Int = 100,
        transactionTimeout: Int = 20000
    ): HttpResponse {
        val apiUrl = this.connectionProperties.apiUrl
        val saveUrl = "$apiUrl/entity/JSON/TREE/$model/$modelVersion"
        return this.ktorClient.post(saveUrl) {
            parameter("transactionWindow", transactionWindow)
            parameter("transactionTimeoutMillis", transactionTimeout)
            contentType(ContentType.Application.Json)
            setBody(obj)
        }
    }

}