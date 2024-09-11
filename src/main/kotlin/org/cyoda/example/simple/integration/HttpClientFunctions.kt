package org.cyoda.example.simple.integration

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*


suspend fun HttpClient.publish(obj: Any,
                               model:String,
                               modelVersion:Int,
                               apiUrl:String,
                               transactionWindow:Int = 100,
                               transactionTimeout:Int = 20000
): HttpResponse {
    val saveUrl = "$apiUrl/entity/JSON/TREE/$model/$modelVersion"
    return this.post(saveUrl) {
        parameter("transactionWindow", transactionWindow)
        parameter("transactionTimeoutMillis", transactionTimeout)
        contentType(ContentType.Application.Json)
        setBody(obj)
    }
}
