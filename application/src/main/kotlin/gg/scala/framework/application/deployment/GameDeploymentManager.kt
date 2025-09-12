package gg.scala.framework.application.deployment

import gg.scala.framework.application.config.ApplicationConfig
import gg.scala.framework.shared.game.GameType
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.*
import io.kubernetes.client.util.Config
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages game server deployment on Kubernetes (k3s).
 * Handles creating, scaling, and managing game server instances.
 */
class GameDeploymentManager {
    
    private val logger = LoggerFactory.getLogger(GameDeploymentManager::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Kubernetes API clients
    private var coreV1Api: CoreV1Api? = null
    private var appsV1Api: AppsV1Api? = null
    
    // Active deployments
    private val activeDeployments = ConcurrentHashMap<String, GameServerDeployment>()
    
    // Deployment configurations by game type
    private val deploymentConfigs = ConcurrentHashMap<GameType, DeploymentConfig>()
    
    /**
     * Initialize the deployment manager with Kubernetes connection
     */
    suspend fun initialize() {
        logger.info("Initializing Game Deployment Manager...")
        
        try {
            // Initialize Kubernetes client
            val client: ApiClient = if (ApplicationConfig.Kubernetes.configPath.isNotEmpty()) {
                Config.fromConfig(ApplicationConfig.Kubernetes.configPath)
            } else {
                Config.defaultClient()
            }
            Configuration.setDefaultApiClient(client)
            
            coreV1Api = CoreV1Api(client)
            appsV1Api = AppsV1Api(client)
            
            // Test connection
            coreV1Api?.listNamespace(null, null, null, null, null, null, null, null, null, null)
            
            logger.info("Game Deployment Manager initialized with Kubernetes connection to namespace: ${ApplicationConfig.Kubernetes.namespace}")
        } catch (e: Exception) {
            logger.error("Failed to initialize Game Deployment Manager", e)
            throw e
        }
    }
    
    /**
     * Deploy a game server
     */
    suspend fun deployGameServer(
        gameType: GameType,
        mapId: String,
        serverId: String,
        playerCount: Int
    ): GameServerDeployment? {
        return try {
            val config = deploymentConfigs[gameType]
                ?: throw IllegalStateException("No deployment config found for game type: $gameType")
            
            val deployment = createGameServerDeployment(
                gameType = gameType,
                mapId = mapId,
                serverId = serverId,
                playerCount = playerCount,
                config = config
            )
            
            // Create Kubernetes deployment
            val k8sDeployment = createKubernetesDeployment(deployment)
            appsV1Api?.createNamespacedDeployment(
                ApplicationConfig.Kubernetes.namespace, 
                k8sDeployment, 
                null, null, null, null
            )
            
            // Create Kubernetes service
            val k8sService = createKubernetesService(deployment)
            coreV1Api?.createNamespacedService(
                ApplicationConfig.Kubernetes.namespace, 
                k8sService, 
                null, null, null, null
            )
            
            activeDeployments[serverId] = deployment
            
            logger.info("Deployed game server $serverId for $gameType on map $mapId")
            deployment
            
        } catch (e: Exception) {
            logger.error("Failed to deploy game server $serverId", e)
            null
        }
    }
    
    /**
     * Scale a game server deployment
     */
    suspend fun scaleGameServer(serverId: String, replicas: Int): Boolean {
        return try {
            val deployment = activeDeployments[serverId]
                ?: throw IllegalStateException("No deployment found for server: $serverId")
            
            // Update Kubernetes deployment
            val k8sDeployment = appsV1Api?.readNamespacedDeployment(
                deployment.deploymentName,
                ApplicationConfig.Kubernetes.namespace,
                null
            )
            
            k8sDeployment?.spec?.replicas = replicas
            appsV1Api?.replaceNamespacedDeployment(
                deployment.deploymentName,
                ApplicationConfig.Kubernetes.namespace,
                k8sDeployment,
                null, null, null, null
            )
            
            deployment.replicas = replicas
            
            logger.info("Scaled game server $serverId to $replicas replicas")
            true
            
        } catch (e: Exception) {
            logger.error("Failed to scale game server $serverId", e)
            false
        }
    }
    
    /**
     * Delete a game server deployment
     */
    suspend fun deleteGameServer(serverId: String): Boolean {
        return try {
            val deployment = activeDeployments[serverId]
                ?: throw IllegalStateException("No deployment found for server: $serverId")
            
            // Delete Kubernetes deployment
            // TODO: Fix Kubernetes API call parameters
            // appsV1Api?.deleteNamespacedDeployment(
            //     deployment.deploymentName,
            //     ApplicationConfig.Kubernetes.namespace,
            //     null, null, null, null, null, null, null
            // )
            
            // Delete Kubernetes service
            coreV1Api?.deleteNamespacedService(
                deployment.serviceName,
                ApplicationConfig.Kubernetes.namespace,
                null, null, null, null, null, null
            )
            
            activeDeployments.remove(serverId)
            
            logger.info("Deleted game server $serverId")
            true
            
        } catch (e: Exception) {
            logger.error("Failed to delete game server $serverId", e)
            false
        }
    }
    
    /**
     * Get deployment status
     */
    suspend fun getDeploymentStatus(serverId: String): DeploymentStatus? {
        return try {
            val deployment = activeDeployments[serverId] ?: return null
            
            val k8sDeployment = appsV1Api?.readNamespacedDeployment(
                deployment.deploymentName,
                ApplicationConfig.Kubernetes.namespace,
                null
            )
            
            val readyReplicas = k8sDeployment?.status?.readyReplicas ?: 0
            val availableReplicas = k8sDeployment?.status?.availableReplicas ?: 0
            
            DeploymentStatus(
                serverId = serverId,
                gameType = deployment.gameType,
                mapId = deployment.mapId,
                replicas = deployment.replicas,
                readyReplicas = readyReplicas,
                availableReplicas = availableReplicas,
                isHealthy = readyReplicas > 0
            )
            
        } catch (e: Exception) {
            logger.error("Failed to get deployment status for $serverId", e)
            null
        }
    }
    
    /**
     * Get all deployment statuses
     */
    suspend fun getAllDeploymentStatuses(): List<DeploymentStatus> {
        val statuses = mutableListOf<DeploymentStatus>()
        
        for (serverId in activeDeployments.keys) {
            getDeploymentStatus(serverId)?.let { statuses.add(it) }
        }
        
        return statuses
    }
    
    /**
     * Configure deployment settings for a game type
     */
    fun configureGameType(gameType: GameType, config: DeploymentConfig) {
        deploymentConfigs[gameType] = config
        logger.info("Configured deployment for game type: $gameType")
    }
    
    /**
     * Start deployment monitoring
     */
    suspend fun startDeploymentMonitoring() {
        logger.info("Starting deployment monitoring...")
        
        // Monitor deployments every 30 seconds
        while (scope.isActive) {
            try {
                monitorDeployments()
                delay(30_000)
            } catch (e: Exception) {
                logger.error("Error monitoring deployments", e)
                delay(60_000) // Wait longer on error
            }
        }
    }
    
    /**
     * Monitor all active deployments
     */
    private suspend fun monitorDeployments() {
        for ((serverId, deployment) in activeDeployments) {
            try {
                val status = getDeploymentStatus(serverId)
                if (status != null && !status.isHealthy) {
                    logger.warn("Deployment $serverId is unhealthy, attempting to restart...")
                    // TODO: Implement restart logic
                }
            } catch (e: Exception) {
                logger.error("Error monitoring deployment $serverId", e)
            }
        }
    }
    
    /**
     * Create a game server deployment object
     */
    private fun createGameServerDeployment(
        gameType: GameType,
        mapId: String,
        serverId: String,
        playerCount: Int,
        config: DeploymentConfig
    ): GameServerDeployment {
        return GameServerDeployment(
            id = UUID.randomUUID(),
            serverId = serverId,
            gameType = gameType,
            mapId = mapId,
            playerCount = playerCount,
            replicas = 1,
            deploymentName = "game-server-$serverId",
            serviceName = "game-server-$serverId",
            config = config
        )
    }
    
    /**
     * Create Kubernetes deployment manifest
     */
    private fun createKubernetesDeployment(deployment: GameServerDeployment): V1Deployment {
        return V1Deployment().apply {
            apiVersion = "apps/v1"
            kind = "Deployment"
            metadata = V1ObjectMeta().apply {
                name = deployment.deploymentName
                labels = mapOf(
                    "app" to "game-server",
                    "game-type" to deployment.gameType.id.lowercase(),
                    "server-id" to deployment.serverId
                )
            }
            spec = V1DeploymentSpec().apply {
                replicas = deployment.replicas
                selector = V1LabelSelector().apply {
                    matchLabels = mapOf("app" to "game-server", "server-id" to deployment.serverId)
                }
                template = V1PodTemplateSpec().apply {
                    metadata = V1ObjectMeta().apply {
                        labels = mapOf("app" to "game-server", "server-id" to deployment.serverId)
                    }
                    spec = V1PodSpec().apply {
                        containers = listOf(
                            V1Container().apply {
                                name = "game-server"
                                image = deployment.config.image
                                ports = listOf(
                                    V1ContainerPort().apply {
                                        containerPort = 25565
                                        name = "minecraft"
                                        protocol = "TCP"
                                    }
                                )
                                env = listOf(
                                    V1EnvVar().apply {
                                        name = "SERVER_ID"
                                        value = deployment.serverId
                                    },
                                    V1EnvVar().apply {
                                        name = "GAME_TYPE"
                                        value = deployment.gameType.id
                                    },
                                    V1EnvVar().apply {
                                        name = "MAP_ID"
                                        value = deployment.mapId
                                    }
                                )
                                resources = V1ResourceRequirements().apply {
                                    requests = mapOf(
                                        "cpu" to io.kubernetes.client.custom.Quantity(deployment.config.resourceLimits.cpu),
                                        "memory" to io.kubernetes.client.custom.Quantity(deployment.config.resourceLimits.memory)
                                    )
                                    limits = mapOf(
                                        "cpu" to io.kubernetes.client.custom.Quantity(deployment.config.resourceLimits.cpu),
                                        "memory" to io.kubernetes.client.custom.Quantity(deployment.config.resourceLimits.memory)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Create Kubernetes service manifest
     */
    private fun createKubernetesService(deployment: GameServerDeployment): V1Service {
        return V1Service().apply {
            apiVersion = "v1"
            kind = "Service"
            metadata = V1ObjectMeta().apply {
                name = deployment.serviceName
                labels = mapOf(
                    "app" to "game-server",
                    "server-id" to deployment.serverId
                )
            }
            spec = V1ServiceSpec().apply {
                selector = mapOf("app" to "game-server", "server-id" to deployment.serverId)
                ports = listOf(
                    V1ServicePort().apply {
                        port = 25565
                        targetPort = io.kubernetes.client.custom.IntOrString(25565)
                        name = "minecraft"
                    }
                )
                type = "ClusterIP"
            }
        }
    }
    
    /**
     * Shutdown the deployment manager
     */
    fun shutdown() {
        logger.info("Shutting down Game Deployment Manager...")
        scope.coroutineContext.cancelChildren()
        activeDeployments.clear()
        deploymentConfigs.clear()
        logger.info("Game Deployment Manager shutdown complete")
    }
}

/**
 * Represents a game server deployment
 */
data class GameServerDeployment(
    val id: UUID,
    val serverId: String,
    val gameType: GameType,
    val mapId: String,
    val playerCount: Int,
    var replicas: Int,
    val deploymentName: String,
    val serviceName: String,
    val config: DeploymentConfig,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Deployment configuration for a game type
 */
data class DeploymentConfig(
    val image: String,
    val resourceLimits: ResourceLimits,
    val autoScale: Boolean = true,
    val maxReplicas: Int = 10,
    val minReplicas: Int = 1
)

/**
 * Resource limits for deployments
 */
data class ResourceLimits(
    val cpu: String = "500m",
    val memory: String = "1Gi"
)

/**
 * Deployment status
 */
data class DeploymentStatus(
    val serverId: String,
    val gameType: GameType,
    val mapId: String,
    val replicas: Int,
    val readyReplicas: Int,
    val availableReplicas: Int,
    val isHealthy: Boolean
)
