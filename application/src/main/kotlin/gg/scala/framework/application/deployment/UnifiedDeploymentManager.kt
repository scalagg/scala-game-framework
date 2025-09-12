package gg.scala.framework.application.deployment

import gg.scala.framework.application.config.ApplicationConfig
import gg.scala.framework.application.game.GameType
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Unified deployment manager that supports both Kubernetes and Docker strategies
 */
class UnifiedDeploymentManager {
    
    private val logger = LoggerFactory.getLogger(UnifiedDeploymentManager::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val deploymentStrategy: DeploymentStrategy
    
    init {
        val strategyType = when (ApplicationConfig.Deployment.strategy.lowercase()) {
            "kubernetes", "k8s" -> DeploymentStrategyType.KUBERNETES
            "docker" -> DeploymentStrategyType.DOCKER
            else -> {
                logger.warn("Unknown deployment strategy: ${ApplicationConfig.Deployment.strategy}, defaulting to Docker")
                DeploymentStrategyType.DOCKER
            }
        }
        
        deploymentStrategy = DeploymentStrategyFactory.createStrategy(strategyType)
        logger.info("Initialized deployment manager with strategy: $strategyType")
    }
    
    /**
     * Initialize the deployment manager
     */
    suspend fun initialize() {
        logger.info("Initializing Unified Deployment Manager...")
        deploymentStrategy.initialize()
        logger.info("Unified Deployment Manager initialized successfully")
    }
    
    /**
     * Deploy a game server
     */
    suspend fun deployGameServer(
        gameType: GameType,
        mapId: String,
        serverId: String,
        playerCount: Int
    ): Any {
        logger.info("Deploying game server for $gameType on map $mapId with ${ApplicationConfig.Deployment.strategy} strategy")
        return deploymentStrategy.deployGameServer(gameType, mapId, serverId, playerCount)
    }
    
    /**
     * Scale a game server
     */
    suspend fun scaleGameServer(serverId: String, replicas: Int): Boolean {
        logger.info("Scaling game server $serverId to $replicas replicas")
        return deploymentStrategy.scaleGameServer(serverId, replicas)
    }
    
    /**
     * Delete a game server
     */
    suspend fun deleteGameServer(serverId: String): Boolean {
        logger.info("Deleting game server $serverId")
        return deploymentStrategy.deleteGameServer(serverId)
    }
    
    /**
     * Get deployment status
     */
    suspend fun getDeploymentStatus(serverId: String): Any? {
        return deploymentStrategy.getDeploymentStatus(serverId)
    }
    
    /**
     * Get all deployment statuses
     */
    suspend fun getAllDeploymentStatuses(): List<Any> {
        return deploymentStrategy.getAllDeploymentStatuses()
    }
    
    /**
     * Start deployment monitoring
     */
    suspend fun startDeploymentMonitoring() {
        logger.info("Starting deployment monitoring with ${ApplicationConfig.Deployment.strategy} strategy")
        deploymentStrategy.startDeploymentMonitoring()
    }
    
    /**
     * Get deployment strategy type
     */
    fun getDeploymentStrategyType(): String {
        return ApplicationConfig.Deployment.strategy
    }
    
    /**
     * Check if auto-scaling is enabled
     */
    fun isAutoScalingEnabled(): Boolean {
        return ApplicationConfig.Deployment.autoScaling
    }
    
    /**
     * Get maximum instances allowed
     */
    fun getMaxInstances(): Int {
        return ApplicationConfig.Deployment.maxInstances
    }
    
    /**
     * Shutdown the deployment manager
     */
    fun shutdown() {
        logger.info("Shutting down Unified Deployment Manager...")
        scope.coroutineContext.cancelChildren()
        deploymentStrategy.shutdown()
        logger.info("Unified Deployment Manager shutdown complete")
    }
}
