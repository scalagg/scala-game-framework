package gg.scala.framework.application

import gg.scala.framework.application.deployment.UnifiedDeploymentManager
import gg.scala.framework.application.game.GameManager
import gg.scala.framework.application.health.HealthCheckManager
import gg.scala.framework.application.metrics.MetricsManager
import gg.scala.framework.application.queue.QueueManager
import gg.scala.framework.application.replication.ReplicationManager
import gg.scala.framework.application.game.GameType
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Main application class for the Minigames Manager.
 * Handles deployment, queue management, leaderboards, and game coordination.
 */
class MinigamesManagerApplication {
    
    private val logger = LoggerFactory.getLogger(MinigamesManagerApplication::class.java)
    private val applicationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Core managers
    private val queueManager = QueueManager()
    private val gameManager = GameManager()
    private val replicationManager = ReplicationManager()
    private val deploymentManager = UnifiedDeploymentManager()
    
    // Monitoring managers
    private val healthCheckManager = HealthCheckManager(
        queueManager, gameManager, replicationManager, deploymentManager
    )
    private val metricsManager = MetricsManager()
    
    // Game type configurations
    private val gameTypeConfigs = ConcurrentHashMap<GameType, GameTypeConfig>()
    
    /**
     * Initialize the application and start all managers
     */
    suspend fun initialize() {
        logger.info("Initializing Minigames Manager Application...")
        
        try {
            // Initialize managers in order
            replicationManager.initialize()
            gameManager.initialize()
            queueManager.initialize()
            deploymentManager.initialize()
            healthCheckManager.initialize()
            metricsManager.initialize()
            
            // Start background tasks
            startBackgroundTasks()
            
            logger.info("Minigames Manager Application initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize Minigames Manager Application", e)
            throw e
        }
    }
    
    /**
     * Start background tasks for the application
     */
    private fun startBackgroundTasks() {
        // Start queue processing
        applicationScope.launch {
            queueManager.startQueueProcessing()
        }
        
        // Start game status monitoring
        applicationScope.launch {
            gameManager.startGameMonitoring()
        }
        
        // Start replication monitoring
        applicationScope.launch {
            replicationManager.startReplicationMonitoring()
        }
        
        // Start deployment monitoring
        applicationScope.launch {
            deploymentManager.startDeploymentMonitoring()
        }
        
        // Start health check monitoring
        applicationScope.launch {
            healthCheckManager.startHealthMonitoring()
        }
        
        // Start metrics collection
        applicationScope.launch {
            metricsManager.startMetricsCollection()
        }
    }
    
    /**
     * Shutdown the application gracefully
     */
    suspend fun shutdown() {
        logger.info("Shutting down Minigames Manager Application...")
        
        applicationScope.coroutineContext.cancelChildren()
        
        // Shutdown managers
        healthCheckManager.shutdown()
        metricsManager.shutdown()
        deploymentManager.shutdown()
        replicationManager.shutdown()
        gameManager.shutdown()
        queueManager.shutdown()

        logger.info("Minigames Manager Application shutdown complete")
    }
    
    /**
     * Get the queue manager
     */
    fun getQueueManager(): QueueManager = queueManager
    
    /**
     * Get the game manager
     */
    fun getGameManager(): GameManager = gameManager
    
    /**
     * Get the replication manager
     */
    fun getReplicationManager(): ReplicationManager = replicationManager
    
    /**
     * Get the deployment manager
     */
    fun getDeploymentManager(): UnifiedDeploymentManager = deploymentManager
    
    /**
     * Get the health check manager
     */
    fun getHealthCheckManager(): HealthCheckManager = healthCheckManager
    
    /**
     * Get the metrics manager
     */
    fun getMetricsManager(): MetricsManager = metricsManager
    
    /**
     * Configure a game type
     */
    fun configureGameType(gameType: GameType, config: GameTypeConfig) {
        gameTypeConfigs[gameType] = config
        logger.info("Configured game type: $gameType")
    }
    
    /**
     * Get game type configuration
     */
    fun getGameTypeConfig(gameType: GameType): GameTypeConfig? = gameTypeConfigs[gameType]
}

/**
 * Configuration for a game type
 */
data class GameTypeConfig(
    val maxPlayers: Int,
    val minPlayers: Int,
    val teamSize: Int,
    val kitRequired: Boolean,
    val matchmakingSettings: MatchmakingSettings,
    val deploymentSettings: DeploymentSettings
)

/**
 * Matchmaking settings for a game type
 */
data class MatchmakingSettings(
    val maxPing: Int = 200,
    val eloRange: Int = 100,
    val allowCrossRegion: Boolean = false
)

/**
 * Deployment settings for a game type
 */
data class DeploymentSettings(
    val maxInstances: Int = 10,
    val autoScale: Boolean = true,
    val resourceLimits: ResourceLimits = ResourceLimits()
)

/**
 * Resource limits for game servers
 */
data class ResourceLimits(
    val cpu: String = "500m",
    val memory: String = "1Gi",
    val storage: String = "2Gi"
)
