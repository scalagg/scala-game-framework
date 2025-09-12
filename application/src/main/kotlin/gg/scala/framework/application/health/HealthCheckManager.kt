package gg.scala.framework.application.health

import gg.scala.framework.application.deployment.UnifiedDeploymentManager
import gg.scala.framework.application.game.GameManager
import gg.scala.framework.application.game.GameType
import gg.scala.framework.application.queue.QueueManager
import gg.scala.framework.application.replication.ReplicationManager
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages health checks for all application components
 */
class HealthCheckManager(
    private val queueManager: QueueManager,
    private val gameManager: GameManager,
    private val replicationManager: ReplicationManager,
    private val deploymentManager: UnifiedDeploymentManager
) {
    
    private val logger = LoggerFactory.getLogger(HealthCheckManager::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val healthChecks = ConcurrentHashMap<String, HealthCheck>()
    
    /**
     * Initialize health check manager
     */
    suspend fun initialize() {
        logger.info("Initializing Health Check Manager...")
        
        // Register health checks
        registerHealthCheck("queue-manager") {
            try {
                // Check if queue manager is responsive
                queueManager.getQueueStats(
                    GameType("test", "Test"), 
                    1, 
                    "default"
                )
                HealthStatus.HEALTHY
            } catch (e: Exception) {
                logger.warn("Queue manager health check failed", e)
                HealthStatus.UNHEALTHY
            }
        }
        
        registerHealthCheck("game-manager") {
            try {
                // Check if game manager is responsive
                gameManager.getAllActiveGames()
                HealthStatus.HEALTHY
            } catch (e: Exception) {
                logger.warn("Game manager health check failed", e)
                HealthStatus.UNHEALTHY
            }
        }
        
        registerHealthCheck("replication-manager") {
            try {
                // Check if replication manager is responsive
                replicationManager.getAllServerHealth()
                HealthStatus.HEALTHY
            } catch (e: Exception) {
                logger.warn("Replication manager health check failed", e)
                HealthStatus.UNHEALTHY
            }
        }
        
        registerHealthCheck("deployment-manager") {
            try {
                // Check if deployment manager is responsive
                deploymentManager.getAllDeploymentStatuses()
                HealthStatus.HEALTHY
            } catch (e: Exception) {
                logger.warn("Deployment manager health check failed", e)
                HealthStatus.UNHEALTHY
            }
        }
        
        logger.info("Health Check Manager initialized")
    }
    
    /**
     * Register a health check
     */
    fun registerHealthCheck(name: String, check: suspend () -> HealthStatus) {
        healthChecks[name] = HealthCheck(name, check)
    }
    
    /**
     * Get overall application health
     */
    suspend fun getApplicationHealth(): ApplicationHealth {
        val componentHealth = mutableMapOf<String, HealthStatus>()
        var overallHealthy = true
        
        for ((name, healthCheck) in healthChecks) {
            try {
                val status = healthCheck.check()
                componentHealth[name] = status
                if (status != HealthStatus.HEALTHY) {
                    overallHealthy = false
                }
            } catch (e: Exception) {
                logger.error("Health check failed for $name", e)
                componentHealth[name] = HealthStatus.UNHEALTHY
                overallHealthy = false
            }
        }
        
        return ApplicationHealth(
            healthy = overallHealthy,
            components = componentHealth,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Start health check monitoring
     */
    suspend fun startHealthMonitoring() {
        logger.info("Starting health check monitoring...")
        
        // Run health checks every 30 seconds
        while (scope.isActive) {
            try {
                val health = getApplicationHealth()
                if (!health.healthy) {
                    logger.warn("Application health check failed: $health")
                } else {
                    logger.debug("Application health check passed")
                }
                delay(30_000)
            } catch (e: Exception) {
                logger.error("Error during health monitoring", e)
                delay(60_000) // Wait longer on error
            }
        }
    }
    
    /**
     * Shutdown health check manager
     */
    fun shutdown() {
        logger.info("Shutting down Health Check Manager...")
        scope.coroutineContext.cancelChildren()
        healthChecks.clear()
        logger.info("Health Check Manager shutdown complete")
    }
}

/**
 * Represents a health check
 */
data class HealthCheck(
    val name: String,
    val check: suspend () -> HealthStatus
)

/**
 * Health status
 */
enum class HealthStatus {
    HEALTHY,
    UNHEALTHY,
    DEGRADED
}

/**
 * Application health status
 */
data class ApplicationHealth(
    val healthy: Boolean,
    val components: Map<String, HealthStatus>,
    val timestamp: Long
)
