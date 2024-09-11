package org.cyoda.example.simple


import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.data.PojoCloudEventData
import io.cloudevents.core.format.EventFormat
import io.cloudevents.core.provider.EventFormatProvider
import io.cloudevents.protobuf.ProtobufFormat
import io.cloudevents.v1.proto.CloudEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.*
import io.grpc.stub.StreamObserver
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.cyoda.cloud.api.event.common.BaseEvent
import org.cyoda.cloud.api.event.common.CloudEventType
import org.cyoda.cloud.api.event.common.Error
import org.cyoda.cloud.api.event.processing.CalculationMemberJoinEvent
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse
import org.cyoda.cloud.api.grpc.CloudEventsServiceGrpc
import org.cyoda.example.simple.config.ClientConnectionProperties
import org.cyoda.example.simple.integration.CyodaHttpClient
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import java.net.URI
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * This is a simple client for demonstration purposes only and should not be used in a production environment
 * as it does not do the necessary configuration for timeouts, error handling, message size config etc.
 */
@Component
class CyodaCalculationMemberClient(
    private val objectMapper: ObjectMapper,
    private val cyodaHttpClient: CyodaHttpClient,
    processors: Map<String, Processor>
) {

    private val processorsByName = processors.values.associateBy { it.name }
    private val delegatingProcessor = DelegatingProcessor(processorsByName, cyodaHttpClient)

    /**
     * This needs to be lazy, so that the assembly is triggered only after the application context is refreshed,
     * and we got the [ContextRefreshedEvent] from our [EventListener]
     */
    private val managedChannel: ManagedChannel by lazy(LazyThreadSafetyMode.NONE) {
        cyodaHttpClient.connectionProperties.createManagedChannel()
    }

    /**
     * Same here...
     */
    private val clientStub: CloudEventsServiceGrpc.CloudEventsServiceStub by lazy(LazyThreadSafetyMode.NONE) {
        managedChannel.createClientStub()
    }


    private val eventFormat: EventFormat = EventFormatProvider
        .getInstance()
        .resolveFormat(ProtobufFormat.PROTO_CONTENT_TYPE)
        ?: throw NullPointerException("Unable to resolve protobuf event format")

    private val callCredentials = object : CallCredentials() {
        private val JWT_METADATA = Metadata.Key.of(HttpHeaders.AUTHORIZATION, Metadata.ASCII_STRING_MARSHALLER)

        override fun applyRequestMetadata(
            requestInfo: RequestInfo,
            executor: Executor,
            metadataApplier: MetadataApplier,
        ) {
            executor.execute {
                try {
                    val headers = Metadata()
                    val accessToken = cyodaHttpClient.getAccessTokenBlocking()
                    headers.put(JWT_METADATA, "Bearer $accessToken")
                    metadataApplier.apply(headers)
                } catch (e: Throwable) {
                    metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e))
                }
            }
        }
    }

    private val streamingObserver: StreamObserver<CloudEvent> = clientStub.createStreamingObserver()

    private final fun ClientConnectionProperties.createManagedChannel() =
        ManagedChannelBuilder
            .forAddress(
                this.grpcServer,
                this.grpcServerPort
            ).also { builder ->
                if (!this.grpcServerUseTls) {
                    builder.usePlaintext()
                }
            }.build()

    private final fun ManagedChannel.createClientStub() = CloudEventsServiceGrpc.newStub(this)
        .withCallCredentials(callCredentials)
        .withWaitForReady()

    @EventListener(ContextRefreshedEvent::class)
    fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent) {
        sendEvent(CalculationMemberJoinEvent().apply {
            this.owner = "CYODA"
            this.tags = listOf("default", "prizes")
        })
    }

    private final fun CloudEventsServiceGrpc.CloudEventsServiceStub.createStreamingObserver(): StreamObserver<CloudEvent> =
        this.startStreaming(object : StreamObserver<CloudEvent> {

            override fun onNext(value: CloudEvent) {
                logger.info { ">> Got EVENT:\n${value.type}" }
                when (value.type) {
                    CloudEventType.ENTITY_PROCESSOR_CALCULATION_REQUEST.value() -> {
                        sendEvent(
                            delegatingProcessor.calculate(
                                objectMapper.readValue(
                                    value.textData,
                                    EntityProcessorCalculationRequest::class.java
                                )
                            )
                        )
                    }

                    else -> {
                        logger.info { "Skipping message of type ${value.type} as no processing required" }
                    }
                }
            }

            override fun onError(t: Throwable) {
                logger.error(t) { ">> Got ERROR from remote backend" }
            }

            override fun onCompleted() {
                logger.info { ">> Got COMPLETE from remote backend" }
            }
        })

    fun sendEvent(event: BaseEvent) {
        val cloudEvent = CloudEvent.parseFrom(
            eventFormat.serialize(
                CloudEventBuilder.v1()
                    .withType(event.javaClass.simpleName)
                    .withSource(URI.create("SimpleSample"))
                    .withId(UUID.randomUUID().toString())
                    .withData(PojoCloudEventData.wrap(event) { objectMapper.writeValueAsBytes(it) })
                    .build()
            )
        )

        logger.info { "<< Sending EVENT:\n${event.logString()}" }

        // stream observer is not thread safe, for production usage this should be managed by some pooling for such cases
        synchronized(streamingObserver) {
            streamingObserver.onNext(cloudEvent)
        }
    }

    @PreDestroy
    fun shutdownGrpcIntegration() {
        logger.info { "Disconnecting from grpc" }
        streamingObserver.onCompleted()
        val isTerminated = managedChannel.shutdown()
            .awaitTermination(10, TimeUnit.SECONDS)
        logger.info {
            val terminationState = if (!isTerminated) "NOT" else ""
            "Disconnected from grpc and managedChannel is $terminationState terminated"
        }
    }

    /**
     * If we have large payloads, logging the event will flood the logs. Let's make it short.
     */
    fun BaseEvent.logString() =
        when (this) {
            is EntityProcessorCalculationResponse -> "${EntityProcessorCalculationResponse::class.simpleName} for entity ${this.entityId} and request ${this.requestId} having errors: ${this.error}"
            else -> this.toString()
        }

}

class DelegatingProcessor(
    private val processors: Map<String, Processor>,
    private val client: CyodaHttpClient
) {
    fun calculate(request: EntityProcessorCalculationRequest) =
        runBlocking(Dispatchers.IO) {
            logger.info { "Start Processing" }
            try {
                processors[request.processorName]
                    ?.process(client.ktorClient, request)
                    ?: request.asResponse().apply {
                        this.error = Error().apply {
                            code = "UNKNOWN PROCESSOR"
                            message = "Processor ${request.processorName} not supported."
                        }
                    }
            } catch (e: Exception) {
                request.asResponse().also {
                    it.error = Error().apply {
                        code = "EXCEPTION"
                        message = e.message
                    }
                }
            }
        }.also {
            logger.info { "Finished processing" }
        }
}