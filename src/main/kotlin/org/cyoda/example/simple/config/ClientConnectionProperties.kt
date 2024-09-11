package org.cyoda.example.simple.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The settings that connect the application to Cyoda as a client compute node.
 */
@ConfigurationProperties(prefix = "cyoda.connection")
class ClientConnectionProperties {

    // Replace with the url for your Cyoda namespace, e.g. https://my-namespace.cyoda.net/api
    var apiUrl: String = "http://localhost:8082/api"

    // These can normally stay at their defaults
    var loginUrl: String = "$apiUrl/auth/login"
    var refreshUrl: String = "$apiUrl/auth/token"

    // Replace with your default user, e.g. demo.user and its password
    // How client nodes securely connect to Cyoda will probably change in the near future.
    var clientId: String = "my-client-id"
    var clientSecret: String = "my-client-secret"

    // Replace this with the url for grpc integration, e.g. https://grpc-my-namespace.cyoda.net/
    var grpcServer = "localhost"
    var grpcServerPort: Int = 443
    var grpcServerUseTls: Boolean = true

}