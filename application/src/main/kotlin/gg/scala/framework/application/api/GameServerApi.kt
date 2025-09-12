package gg.scala.framework.application.api

import gg.scala.framework.application.deployment.UnifiedDeploymentManager
import gg.scala.framework.shared.game.GameType
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * API for managing game servers from Spigot/Velocity plugins
 */
class GameServerApi(
    private val deploymentManager: UnifiedDeploymentManager
) {
    
    private val logger = LoggerFactory.getLogger(GameServerApi::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Track active game servers
    private val activeGameServers = ConcurrentHashMap<String, GameServerInfo>()
    
    /**
     * Initialize the API
     */
    suspend fun initialize() {
        logger.info("Initializing Game Server API...")
        // Start monitoring active servers
        scope.launch {
            monitorActiveServers()
        }
        logger.info("Game Server API initialized")
    }
    
    /**
     * Create a new game server
     */
    suspend fun createGameServer(
        gameType: GameType,
        mapId: String,
        playerCount: Int,
        creatorId: String,
        metadata: Map<String, String> = emptyMap()
    ): GameServerInfo {
        val serverId = generateServerId(gameType, mapId)
        
        logger.info("Creating game server: $serverId for $gameType on map $mapId")
        
        try {
            // Deploy the server using the deployment manager
            val deployment = deploymentManager.deployGameServer(
                gameType = gameType,
                mapId = mapId,
                serverId = serverId,
                playerCount = playerCount
            )
            
            // Create server info
            val serverInfo = GameServerInfo(
                serverId = serverId,
                gameType = gameType,
                mapId = mapId,
                playerCount = playerCount,
                creatorId = creatorId,
                status = GameServerStatus.CREATING,
                createdAt = System.currentTimeMillis(),
                metadata = metadata,
                connectionInfo = null // Will be populated when server is ready
            )
            
            activeGameServers[serverId] = serverInfo
            
            // Start monitoring this server
            scope.launch {
                monitorServerStatus(serverInfo)
            }
            
            logger.info("Game server $serverId created successfully")
            return serverInfo
            
        } catch (e: Exception) {
            logger.error("Failed to create game server $serverId", e)
            throw GameServerException("Failed to create game server: ${e.message}", e)
        }
    }
    
    /**
     * Destroy a game server
     */
    suspend fun destroyGameServer(serverId: String): Boolean {
        logger.info("Destroying game server: $serverId")
        
        val serverInfo = activeGameServers[serverId]
            ?: throw GameServerException("Server $serverId not found")
        
        try {
            // Delete the deployment
            val success = deploymentManager.deleteGameServer(serverId)
            
            if (success) {
                // Update server status
                serverInfo.status = GameServerStatus.DESTROYED
                serverInfo.destroyedAt = System.currentTimeMillis()
                
                // Remove from active servers
                activeGameServers.remove(serverId)
                
                logger.info("Game server $serverId destroyed successfully")
                return true
            } else {
                logger.error("Failed to destroy game server $serverId")
                return false
            }
            
        } catch (e: Exception) {
            logger.error("Failed to destroy game server $serverId", e)
            throw GameServerException("Failed to destroy game server: ${e.message}", e)
        }
    }
    
    /**
     * Get server information
     */
    fun getServerInfo(serverId: String): GameServerInfo? {
        return activeGameServers[serverId]
    }
    
    /**
     * Get all active servers
     */
    fun getAllActiveServers(): List<GameServerInfo> {
        return activeGameServers.values.toList()
    }
    
    /**
     * Get servers by game type
     */
    fun getServersByGameType(gameType: GameType): List<GameServerInfo> {
        return activeGameServers.values.filter { it.gameType == gameType }
    }
    
    /**
     * Get servers by status
     */
    fun getServersByStatus(status: GameServerStatus): List<GameServerInfo> {
        return activeGameServers.values.filter { it.status == status }
    }
    
    /**
     * Update server metadata
     */
    fun updateServerMetadata(serverId: String, metadata: Map<String, String>) {
        val serverInfo = activeGameServers[serverId]
        if (serverInfo != null) {
            serverInfo.metadata.putAll(metadata)
            logger.debug("Updated metadata for server $serverId")
        }
    }
    
    /**
     * Monitor active servers
     */
    private suspend fun monitorActiveServers() {
        while (scope.isActive) {
            try {
                // Check for servers that have been in CREATING status too long
                val stuckServers = activeGameServers.values.filter { 
                    it.status == GameServerStatus.CREATING && 
                    System.currentTimeMillis() - it.createdAt > 300000 // 5 minutes
                }
                
                for (server in stuckServers) {
                    logger.warn("Server ${server.serverId} has been in CREATING status for too long, marking as FAILED")
                    server.status = GameServerStatus.FAILED
                }
                
                delay(30_000) // Check every 30 seconds
            } catch (e: Exception) {
                logger.error("Error monitoring active servers", e)
                delay(60_000)
            }
        }
    }
    
    /**
     * Monitor individual server status
     */
    private suspend fun monitorServerStatus(serverInfo: GameServerInfo) {
        while (scope.isActive && activeGameServers.containsKey(serverInfo.serverId)) {
            try {
                val deploymentStatus = deploymentManager.getDeploymentStatus(serverInfo.serverId)
                
                if (deploymentStatus != null) {
                    // Update server status based on deployment status
                    when {
                        deploymentStatus.toString().contains("Running") -> {
                            if (serverInfo.status == GameServerStatus.CREATING) {
                                serverInfo.status = GameServerStatus.RUNNING
                                serverInfo.connectionInfo = extractConnectionInfo(deploymentStatus)
                                logger.info("Server ${serverInfo.serverId} is now running")
                            }
                        }
                        deploymentStatus.toString().contains("Failed") -> {
                            serverInfo.status = GameServerStatus.FAILED
                            logger.error("Server ${serverInfo.serverId} failed to start")
                        }
                        deploymentStatus.toString().contains("Degraded") -> {
                            serverInfo.status = GameServerStatus.DEGRADED
                            logger.warn("Server ${serverInfo.serverId} is degraded")
                        }
                    }
                } else {
                    // Deployment not found, server might have been destroyed
                    if (serverInfo.status != GameServerStatus.DESTROYED) {
                        serverInfo.status = GameServerStatus.DESTROYED
                        activeGameServers.remove(serverInfo.serverId)
                        logger.info("Server ${serverInfo.serverId} deployment not found, marking as destroyed")
                    }
                }
                
                delay(10_000) // Check every 10 seconds
            } catch (e: Exception) {
                logger.error("Error monitoring server ${serverInfo.serverId}", e)
                delay(30_000)
            }
        }
    }
    
    /**
     * Extract connection info from deployment status
     */
    private fun extractConnectionInfo(deploymentStatus: Any): ConnectionInfo? {
        // This would extract the actual connection details from the deployment status
        // For now, return a placeholder
        return ConnectionInfo(
            host = "localhost",
            port = 25565,
            protocol = "TCP"
        )
    }
    
    /**
     * Generate unique server ID
     */
    private fun generateServerId(gameType: GameType, mapId: String): String {
        return "${gameType.id}-${mapId}-${System.currentTimeMillis()}-${UUID.randomUUID().toString().substring(0, 8)}"
    }
    
    /**
     * Shutdown the API
     */
    fun shutdown() {
        logger.info("Shutting down Game Server API...")
        scope.coroutineContext.cancelChildren()
        activeGameServers.clear()
        logger.info("Game Server API shutdown complete")
    }
}

/**
 * Information about a game server
 */
data class GameServerInfo(
    val serverId: String,
    val gameType: GameType,
    val mapId: String,
    val playerCount: Int,
    val creatorId: String,
    var status: GameServerStatus,
    val createdAt: Long,
    var destroyedAt: Long? = null,
    val metadata: MutableMap<String, String> = mutableMapOf(),
    var connectionInfo: ConnectionInfo? = null
)

/**
 * Game server status
 */
enum class GameServerStatus {
    CREATING,
    RUNNING,
    DEGRADED,
    FAILED,
    DESTROYED
}

/**
 * Connection information for a game server
 */
data class ConnectionInfo(
    val host: String,
    val port: Int,
    val protocol: String
)

/**
 * Exception for game server operations
 */
class GameServerException(message: String, cause: Throwable? = null) : Exception(message, cause)
