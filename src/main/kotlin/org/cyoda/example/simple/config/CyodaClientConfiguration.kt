package org.cyoda.example.simple.config

import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import org.cyoda.example.simple.integration.CyodaHttpClient
import org.cyoda.example.simple.integration.TokenManager
import org.cyoda.example.simple.integration.HttpClientSupplier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CyodaClientConfiguration {

    /**
     * A Ktor [HttpClient] that has connected
     * to the Cyoda service with the injected [ClientConnectionProperties]
     *
     * The [ClientConnectionProperties.clientSecret] is erased once the client
     * is instantiated as a cheap trick to remove the secret from the heap.
     *
     * A side effect is that the Cyoda service must be accessible on startup of
     * this Spring Boot application
     *
     * The application will work as long as the refresh token is valid. Once the refresh
     * expires, the compute node must be restarted, since there is no way to recover from that
     * in this simple example
     */
    @Bean
    fun httpClient(
        p: ClientConnectionProperties,
        tokenClient:TokenManager
    ): CyodaHttpClient {
        try {
            return runBlocking {
                HttpClientSupplier(p)
                    .connect(tokenClient,p.clientId, p.clientSecret)
            }
        } finally {
            p.clientSecret = "secret erased"
        }
    }
}