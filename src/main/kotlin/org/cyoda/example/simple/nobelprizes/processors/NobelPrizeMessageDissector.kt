package org.cyoda.example.simple.nobelprizes.processors

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.cyoda.cloud.api.event.common.DataPayload
import org.cyoda.cloud.api.event.common.Error
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse
import org.cyoda.example.simple.Processor
import org.cyoda.example.simple.asResponse
import org.cyoda.example.simple.config.ClientConnectionProperties
import org.cyoda.example.simple.integration.publish
import org.cyoda.example.simple.nobelprizes.model.NobelPrize
import org.springframework.stereotype.Component
import java.util.*

val log = KotlinLogging.logger { }

@Component
class NobelPrizeMessageDissector(val connectionProps: ClientConnectionProperties) : Processor {
    override val name = "Process Nobel Prize Dataset"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }


    override suspend fun process(client: HttpClient,request: EntityProcessorCalculationRequest): EntityProcessorCalculationResponse =
        coroutineScope {

            require(request.payload.data is ObjectNode) {
                "Unexpected payload shape. Expected ${ObjectNode::class.simpleName}, " +
                        "got ${request.payload.data.javaClass}"
            }

            val allPrizes = request.payload.data["data"]["prizes"]

            require(allPrizes is ArrayNode) {
                "Unexpected payload shape. Expected ${ArrayNode::class.simpleName}, " +
                        "but got ${allPrizes.javaClass}"
            }


            var failed = false

            // Let's link this Nobel Prize to its dataset
            val dataSetId = UUID.fromString(request.entityId)

            allPrizes.asFlowOfChunkedNobelPrizes(dataSetId,1200).map { prizes: List<NobelPrize> ->
                async {
                    log.info { "Publishing ${prizes.size} Nobel prizes in a single transaction" }
                    prizes to client.publish(prizes,"prize",1,connectionProps.apiUrl)
                }
            }.collect {
                val result = it.await()
                val prizes = result.first
                val text = result.second.bodyAsText()
                val ok = result.second.status == HttpStatusCode.OK
                if (!ok) {
                    log.warn { "failed to publish ${prizes.size}. Response: $text" }
                    failed = true
                } else {
                    log.debug { "published ${prizes.size} prizes" }
                }
            }

            if (failed) {
                request.asResponse().also {
                    val errorMsg = Error().apply {
                        code = "SAVE FAILED"
                        message = "Save Failed"
                    }
                    log.warn { "Returning calc response with error message $errorMsg" }
                    it.error = errorMsg
                }
            } else {
                request.asResponse().also {
                    it.payload = DataPayload().apply {
                        type = "TREE" // TODO: Replace with EventType.TREE once client spi is available
                        this.data = request.payload.data // Return unchanged
                    }
                }
            }
        }

    private fun ArrayNode.asFlowOfChunkedNobelPrizes(dataSetId: UUID,  chunkSize: Int): Flow<List<NobelPrize>> = this
        .asSequence()
        .chunked(chunkSize)
        .asFlow()
        .map { list ->
            list.map {
                json.decodeFromString<NobelPrize>(it.toString()).withDataSetId(dataSetId)
            }
        }
}