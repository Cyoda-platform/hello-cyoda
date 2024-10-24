package org.cyoda.example.simple

import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse

fun EntityProcessorCalculationRequest.asResponse() =
    EntityProcessorCalculationResponse().also {
        it.id = this.id
        it.entityId = this.entityId
        it.requestId = this.requestId
    }