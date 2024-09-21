package org.cyoda.example.simple

import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse
import org.cyoda.example.simple.integration.CyodaHttpClient

interface Processor {

    val name:String
    suspend fun process(client: CyodaHttpClient, request: EntityProcessorCalculationRequest): EntityProcessorCalculationResponse
}