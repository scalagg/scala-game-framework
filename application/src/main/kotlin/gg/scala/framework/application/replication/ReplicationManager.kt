package gg.scala.framework.application.replication

import gg.scala.framework.shared.map.Map
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages map replications for each game server.
 * Tracks available replications and marks servers as unhealthy if no status update
 * is received within 2 seconds.
 */
class ReplicationManager {
    
    private val logger = LoggerFactory.getLogger(ReplicationManager::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Server health tracking
    private val serverHealth = ConcurrentHashMap<String, ServerHealth>()
    
    // Available replications by server
    private val serverReplications = ConcurrentHashMap<String, MutableSet<MapReplication>>()
    
    // Replication status timeout (2 seconds)
    private val statusTimeout = 2000L
    
    /**
     * Initialize the replication manager
     */
    suspend fun initialize() {
        logger.info("Initializing Replication Manager...")
        // Initialize any required resources here
        logger.info("Replication Manager initialized")
    }
    
    /**
     * Register a server
     */
    fun registerServer(serverId: String) {
        serverHealth[serverId] = ServerHealth(serverId, true, System.currentTimeMillis())
        serverReplications[serverId] = mutableSetOf()
        
        logger.info("Registered server $serverId")
    }
    
    /**
     * Update server status with available replications
     */
    fun updateServerStatus(serverId: String, replications: List<MapReplication>) {
        val health = serverHealth[serverId]
        if (health != null) {
            health.updateLastSeen()
            serverReplications[serverId]?.clear()
            serverReplications[serverId]?.addAll(replications)
            
            logger.debug("Updated server $serverId with ${replications.size} replications")
        } else {
            logger.warn("Attempted to update status for unregistered server $serverId")
        }
    }
    
    /**
     * Get available replications for a specific map
     */
    fun getAvailableReplications(mapId: String): List<MapReplication> {
        val availableReplications = mutableListOf<MapReplication>()
        
        for ((serverId, replications) in serverReplications) {
            val health = serverHealth[serverId]
            if (health?.isHealthy == true) {
                replications.filter { it.mapId == mapId && it.isAvailable }
                    .forEach { availableReplications.add(it) }
            }
        }
        
        return availableReplications
    }
    
    /**
     * Get available replications for a specific Map object
     */
    fun getAvailableReplications(map: gg.scala.framework.shared.map.Map): List<MapReplication> {
        return getAvailableReplications(map.name)
    }
    
    /**
     * Get all available replications
     */
    fun getAllAvailableReplications(): List<MapReplication> {
        val availableReplications = mutableListOf<MapReplication>()
        
        for ((serverId, replications) in serverReplications) {
            val health = serverHealth[serverId]
            if (health?.isHealthy == true) {
                replications.filter { it.isAvailable }
                    .forEach { availableReplications.add(it) }
            }
        }
        
        return availableReplications
    }
    
    /**
     * Reserve a replication for a game
     */
    fun reserveReplication(replicationId: UUID, gameId: UUID): Boolean {
        for ((serverId, replications) in serverReplications) {
            val health = serverHealth[serverId]
            if (health?.isHealthy == true) {
                val replication = replications.find { it.id == replicationId }
                if (replication != null && replication.isAvailable) {
                    replication.reserve(gameId)
                    logger.info("Reserved replication $replicationId for game $gameId")
                    return true
                }
            }
        }
        
        logger.warn("Failed to reserve replication $replicationId for game $gameId")
        return false
    }
    
    /**
     * Release a replication
     */
    fun releaseReplication(replicationId: UUID) {
        for (replications in serverReplications.values) {
            val replication = replications.find { it.id == replicationId }
            if (replication != null) {
                replication.release()
                logger.info("Released replication $replicationId")
                return
            }
        }
        
        logger.warn("Failed to release replication $replicationId")
    }
    
    /**
     * Get server health status
     */
    fun getServerHealth(serverId: String): ServerHealth? = serverHealth[serverId]
    
    /**
     * Get all server health statuses
     */
    fun getAllServerHealth(): kotlin.collections.Map<String, ServerHealth> = serverHealth.toMap()
    
    /**
     * Get healthy servers
     */
    fun getHealthyServers(): List<String> {
        return serverHealth.values
            .filter { it.isHealthy }
            .map { it.serverId }
    }
    
    /**
     * Get unhealthy servers
     */
    fun getUnhealthyServers(): List<String> {
        return serverHealth.values
            .filter { !it.isHealthy }
            .map { it.serverId }
    }
    
    /**
     * Start replication monitoring
     */
    suspend fun startReplicationMonitoring() {
        logger.info("Starting replication monitoring...")
        
        // Check server health every 1 second
        while (scope.isActive) {
            try {
                checkServerHealth()
                delay(1000)
            } catch (e: Exception) {
                logger.error("Error monitoring replications", e)
                delay(5000) // Wait longer on error
            }
        }
    }
    
    /**
     * Check server health and mark unhealthy servers
     */
    private fun checkServerHealth() {
        val currentTime = System.currentTimeMillis()
        
        for ((serverId, health) in serverHealth) {
            val timeSinceLastUpdate = currentTime - health.lastSeen
            if (timeSinceLastUpdate > statusTimeout && health.isHealthy) {
                health.markUnhealthy()
                logger.warn("Server $serverId marked as unhealthy (${timeSinceLastUpdate}ms since last update)")
                
                // Release all replications on this server
                serverReplications[serverId]?.forEach { replication ->
                    if (replication.isReserved) {
                        replication.release()
                        logger.info("Released replication ${replication.id} from unhealthy server $serverId")
                    }
                }
            } else if (timeSinceLastUpdate <= statusTimeout && !health.isHealthy) {
                health.markHealthy()
                logger.info("Server $serverId marked as healthy again")
            }
        }
    }
    
    /**
     * Get replication statistics
     */
    fun getReplicationStatistics(): ReplicationStatistics {
        val totalReplications = serverReplications.values.sumOf { it.size }
        val availableReplications = serverReplications.values.sumOf { 
            it.count { replication -> replication.isAvailable } 
        }
        val reservedReplications = serverReplications.values.sumOf { 
            it.count { replication -> replication.isReserved } 
        }
        val healthyServers = serverHealth.values.count { it.isHealthy }
        val totalServers = serverHealth.size
        
        return ReplicationStatistics(
            totalReplications = totalReplications,
            availableReplications = availableReplications,
            reservedReplications = reservedReplications,
            healthyServers = healthyServers,
            totalServers = totalServers
        )
    }
    
    /**
     * Shutdown the replication manager
     */
    fun shutdown() {
        logger.info("Shutting down Replication Manager...")
        scope.coroutineContext.cancelChildren()
        serverHealth.clear()
        serverReplications.clear()
        logger.info("Replication Manager shutdown complete")
    }
}

/**
 * Represents server health status
 */
class ServerHealth(
    val serverId: String,
    var isHealthy: Boolean,
    var lastSeen: Long
) {
    fun updateLastSeen() {
        lastSeen = System.currentTimeMillis()
    }
    
    fun markHealthy() {
        isHealthy = true
        updateLastSeen()
    }
    
    fun markUnhealthy() {
        isHealthy = false
    }
}

/**
 * Represents a map replication on a server
 */
class MapReplication(
    val id: UUID,
    val mapId: String,
    val serverId: String,
    val mapName: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    private var _isReserved: Boolean = false
    private var _reservedFor: UUID? = null
    
    val isAvailable: Boolean get() = !_isReserved
    val isReserved: Boolean get() = _isReserved
    val reservedFor: UUID? get() = _reservedFor
    
    fun reserve(gameId: UUID) {
        if (!_isReserved) {
            _isReserved = true
            _reservedFor = gameId
        }
    }
    
    fun release() {
        _isReserved = false
        _reservedFor = null
    }
}

/**
 * Replication statistics
 */
data class ReplicationStatistics(
    val totalReplications: Int,
    val availableReplications: Int,
    val reservedReplications: Int,
    val healthyServers: Int,
    val totalServers: Int
)
