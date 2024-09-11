package org.cyoda.example.simple

import io.ktor.client.*
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse

interface Processor {

    val name:String
    suspend fun process(client: HttpClient, request: EntityProcessorCalculationRequest): EntityProcessorCalculationResponse
}