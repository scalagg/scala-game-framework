package gg.scala.framework.examples.integration

import gg.scala.framework.application.api.GameServerApi
import gg.scala.framework.application.api.GameServerInfo
import gg.scala.framework.application.api.GameServerStatus
import gg.scala.framework.application.deployment.UnifiedDeploymentManager
import gg.scala.framework.shared.game.GameType
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration
import java.util.*

/**
 * Example showing how to integrate with the Minigames Manager Application
 * This demonstrates the complete flow from plugin to application
 */
class GameServerIntegrationExample {
    
    private val logger = LoggerFactory.getLogger(GameServerIntegrationExample::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // HTTP client for communicating with the minigames manager application
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    // Configuration
    private val minigamesManagerUrl = "http://localhost:8080" // URL of the minigames manager application
    private val apiKey = "your-api-key-here" // API key for authentication
    
    /**
     * Example 1: Creating a BedWars game server from a Spigot plugin
     */
    suspend fun createBedWarsGameFromSpigot(): GameServerInfo {
        logger.info("Creating BedWars game server from Spigot plugin...")
        
        val gameType = GameType(
            id = "bedwars",
            displayName = "BedWars",
            dataStore = "cgs-bedwars"
        )
        
        val mapId = "bedwars_map_1"
        val playerCount = 16
        val creatorId = "spigot-plugin"
        
        // Method 1: Direct API call (if running in the same JVM)
        // val deploymentManager = UnifiedDeploymentManager()
        // val gameServerApi = GameServerApi(deploymentManager)
        // return gameServerApi.createGameServer(gameType, mapId, playerCount, creatorId)
        
        // Method 2: HTTP API call (if running as separate service)
        return createGameServerViaHttp(gameType, mapId, playerCount, creatorId)
    }
    
    /**
     * Example 2: Creating a SkyWars game server from a Velocity plugin
     */
    suspend fun createSkyWarsGameFromVelocity(): GameServerInfo {
        logger.info("Creating SkyWars game server from Velocity plugin...")
        
        val gameType = GameType(
            id = "skywars",
            displayName = "SkyWars",
            dataStore = "cgs-skywars"
        )
        
        val mapId = "skywars_map_2"
        val playerCount = 12
        val creatorId = "velocity-plugin"
        
        return createGameServerViaHttp(gameType, mapId, playerCount, creatorId)
    }
    
    /**
     * Example 3: Creating a Duels game server with custom metadata
     */
    suspend fun createDuelsGameWithMetadata(): GameServerInfo {
        logger.info("Creating Duels game server with custom metadata...")
        
        val gameType = GameType(
            id = "duels",
            displayName = "Duels",
            dataStore = "cgs-duels"
        )
        
        val mapId = "duels_map_3"
        val playerCount = 2
        val creatorId = "duels-plugin"
        
        val metadata = mapOf(
            "kit" to "diamond_sword",
            "time_limit" to "300",
            "allow_spectators" to "true",
            "ranked" to "false"
        )
        
        return createGameServerViaHttp(gameType, mapId, playerCount, creatorId, metadata)
    }
    
    /**
     * Example 4: Destroying a game server
     */
    suspend fun destroyGameServer(serverId: String): Boolean {
        logger.info("Destroying game server: $serverId")
        
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$minigamesManagerUrl/api/gameservers/$serverId"))
                .header("Authorization", "Bearer $apiKey")
                .DELETE()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() == 200) {
                logger.info("Game server $serverId destroyed successfully")
                return true
            } else {
                logger.error("Failed to destroy game server $serverId: ${response.statusCode()}")
                return false
            }
            
        } catch (e: Exception) {
            logger.error("Error destroying game server $serverId", e)
            return false
        }
    }
    
    /**
     * Example 5: Listing all active game servers
     */
    suspend fun listActiveGameServers(): List<GameServerInfo> {
        logger.info("Listing all active game servers...")
        
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$minigamesManagerUrl/api/gameservers"))
                .header("Authorization", "Bearer $apiKey")
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() == 200) {
                // Parse JSON response to GameServerInfo objects
                // In a real implementation, you'd use a JSON library like Jackson or Gson
                logger.info("Retrieved ${response.body().length} characters of server data")
                
                // For this example, return empty list
                return emptyList()
            } else {
                logger.error("Failed to list game servers: ${response.statusCode()}")
                return emptyList()
            }
            
        } catch (e: Exception) {
            logger.error("Error listing game servers", e)
            return emptyList()
        }
    }
    
    /**
     * Example 6: Getting server information
     */
    suspend fun getServerInfo(serverId: String): GameServerInfo? {
        logger.info("Getting server information for: $serverId")
        
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$minigamesManagerUrl/api/gameservers/$serverId"))
                .header("Authorization", "Bearer $apiKey")
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() == 200) {
                // Parse JSON response to GameServerInfo object
                logger.info("Retrieved server information for $serverId")
                
                // For this example, return null
                return null
            } else if (response.statusCode() == 404) {
                logger.info("Server $serverId not found")
                return null
            } else {
                logger.error("Failed to get server info: ${response.statusCode()}")
                return null
            }
            
        } catch (e: Exception) {
            logger.error("Error getting server info for $serverId", e)
            return null
        }
    }
    
    /**
     * Example 7: Monitoring server status
     */
    suspend fun monitorServerStatus(serverId: String) {
        logger.info("Starting to monitor server status for: $serverId")
        
        while (scope.isActive) {
            try {
                val serverInfo = getServerInfo(serverId)
                
                if (serverInfo == null) {
                    logger.warn("Server $serverId not found, stopping monitoring")
                    break
                }
                
                when (serverInfo.status) {
                    GameServerStatus.RUNNING -> {
                        logger.info("Server $serverId is running")
                        // Server is healthy, continue monitoring
                    }
                    GameServerStatus.CREATING -> {
                        logger.info("Server $serverId is still being created...")
                        // Server is still being created, continue monitoring
                    }
                    GameServerStatus.DEGRADED -> {
                        logger.warn("Server $serverId is degraded")
                        // Server is having issues, might need intervention
                    }
                    GameServerStatus.FAILED -> {
                        logger.error("Server $serverId has failed")
                        // Server failed, stop monitoring
                        break
                    }
                    GameServerStatus.DESTROYED -> {
                        logger.info("Server $serverId has been destroyed")
                        // Server was destroyed, stop monitoring
                        break
                    }
                }
                
                delay(10_000) // Check every 10 seconds
                
            } catch (e: Exception) {
                logger.error("Error monitoring server $serverId", e)
                delay(30_000) // Wait longer on error
            }
        }
    }
    
    /**
     * Example 8: Complete game server lifecycle
     */
    suspend fun completeGameServerLifecycle() {
        logger.info("Starting complete game server lifecycle example...")
        
        try {
            // 1. Create a game server
            val gameType = GameType("bedwars", "BedWars", "cgs-bedwars")
            val serverInfo = createGameServerViaHttp(gameType, "test_map", 16, "lifecycle-test")
            
            if (serverInfo == null) {
                logger.error("Failed to create game server")
                return
            }
            
            logger.info("Created game server: ${serverInfo.serverId}")
            
            // 2. Monitor the server until it's running
            var attempts = 0
            val maxAttempts = 30 // 5 minutes max
            
            while (attempts < maxAttempts) {
                val currentInfo = getServerInfo(serverInfo.serverId)
                
                if (currentInfo?.status == GameServerStatus.RUNNING) {
                    logger.info("Game server ${serverInfo.serverId} is now running!")
                    break
                } else if (currentInfo?.status == GameServerStatus.FAILED) {
                    logger.error("Game server ${serverInfo.serverId} failed to start")
                    return
                }
                
                logger.info("Waiting for server to start... (${currentInfo?.status})")
                delay(10_000) // Wait 10 seconds
                attempts++
            }
            
            if (attempts >= maxAttempts) {
                logger.error("Game server ${serverInfo.serverId} took too long to start")
                return
            }
            
            // 3. Let the server run for a while
            logger.info("Game server is running, waiting 2 minutes...")
            delay(120_000) // Wait 2 minutes
            
            // 4. Destroy the server
            val destroyed = destroyGameServer(serverInfo.serverId)
            
            if (destroyed) {
                logger.info("Game server lifecycle completed successfully!")
            } else {
                logger.error("Failed to destroy game server")
            }
            
        } catch (e: Exception) {
            logger.error("Error in game server lifecycle", e)
        }
    }
    
    /**
     * Create a game server via HTTP API
     */
    private suspend fun createGameServerViaHttp(
        gameType: GameType,
        mapId: String,
        playerCount: Int,
        creatorId: String,
        metadata: Map<String, String> = emptyMap()
    ): GameServerInfo? {
        try {
            val requestBody = """
                {
                    "gameType": {
                        "id": "${gameType.id}",
                        "displayName": "${gameType.displayName}",
                        "dataStore": "${gameType.dataStore}"
                    },
                    "mapId": "$mapId",
                    "playerCount": $playerCount,
                    "creatorId": "$creatorId",
                    "metadata": ${metadata.entries.joinToString(", ") { "\"${it.key}\": \"${it.value}\"" }}
                }
            """.trimIndent()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$minigamesManagerUrl/api/gameservers"))
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() == 201) {
                logger.info("Game server created successfully")
                // Parse JSON response to GameServerInfo object
                // In a real implementation, you'd use a JSON library
                return null // Placeholder
            } else {
                logger.error("Failed to create game server: ${response.statusCode()}")
                return null
            }
            
        } catch (e: Exception) {
            logger.error("Error creating game server via HTTP", e)
            return null
        }
    }
    
    /**
     * Shutdown the integration example
     */
    fun shutdown() {
        logger.info("Shutting down Game Server Integration Example...")
        scope.coroutineContext.cancelChildren()
        logger.info("Game Server Integration Example shutdown complete")
    }
}

/**
 * Main function to run the examples
 */
suspend fun main() {
    val example = GameServerIntegrationExample()
    
    try {
        // Run the complete lifecycle example
        example.completeGameServerLifecycle()
        
        // Run individual examples
        // example.createBedWarsGameFromSpigot()
        // example.createSkyWarsGameFromVelocity()
        // example.createDuelsGameWithMetadata()
        
    } finally {
        example.shutdown()
    }
}
