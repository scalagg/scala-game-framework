package gg.scala.framework.application.deployment

import gg.scala.framework.application.config.ApplicationConfig
import gg.scala.framework.application.game.GameType
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages game server deployment using Docker.
 * Alternative to Kubernetes deployment for simpler setups.
 */
class DockerDeploymentManager {
    
    private val logger = LoggerFactory.getLogger(DockerDeploymentManager::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Active deployments
    private val activeDeployments = ConcurrentHashMap<String, DockerGameDeployment>()
    
    // Deployment configurations
    private val deploymentConfigs = ConcurrentHashMap<GameType, DockerDeploymentConfig>()
    
    // Docker network name
    private val networkName = ApplicationConfig.Docker.networkName
    
    /**
     * Initialize the Docker deployment manager
     */
    suspend fun initialize() {
        logger.info("Initializing Docker Deployment Manager...")
        
        try {
            // Create Docker network if it doesn't exist
            createDockerNetwork()
            
            logger.info("Docker Deployment Manager initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize Docker Deployment Manager", e)
            throw e
        }
    }
    
    /**
     * Deploy a game server using Docker
     */
    suspend fun deployGameServer(
        gameType: GameType,
        mapId: String,
        playerCount: Int,
        config: DockerDeploymentConfig
    ): DockerGameDeployment {
        val serverId = generateServerId(gameType, mapId)
        
        logger.info("Deploying Docker game server $serverId for $gameType on map $mapId")
        
        val deployment = DockerGameDeployment(
            serverId = serverId,
            gameType = gameType,
            mapId = mapId,
            playerCount = playerCount,
            config = config,
            containerName = "game-server-$serverId",
            networkName = networkName
        )
        
        // Create Docker Compose file
        val composeFile = createDockerComposeFile(deployment)
        
        // Start Docker container
        startDockerContainer(deployment, composeFile)
        
        activeDeployments[serverId] = deployment
        
        logger.info("Deployed Docker game server $serverId for $gameType on map $mapId")
        return deployment
    }
    
    /**
     * Scale a game server (start/stop containers)
     */
    suspend fun scaleGameServer(serverId: String, replicas: Int): Boolean {
        return try {
            val deployment = activeDeployments[serverId]
                ?: throw IllegalStateException("No deployment found for server: $serverId")
            
            if (replicas > 0) {
                // Start additional containers
                for (i in deployment.replicas until replicas) {
                    val containerName = "${deployment.containerName}-$i"
                    startDockerContainer(deployment, null, containerName)
                }
            } else {
                // Stop all containers
                stopDockerContainers(deployment)
            }
            
            deployment.replicas = replicas
            
            logger.info("Scaled Docker game server $serverId to $replicas replicas")
            true
        } catch (e: Exception) {
            logger.error("Failed to scale Docker game server $serverId", e)
            false
        }
    }
    
    /**
     * Delete a game server
     */
    suspend fun deleteGameServer(serverId: String): Boolean {
        return try {
            val deployment = activeDeployments[serverId]
                ?: throw IllegalStateException("No deployment found for server: $serverId")
            
            // Stop and remove containers
            stopDockerContainers(deployment)
            removeDockerContainers(deployment)
            
            // Remove compose file
            val composeFile = File("${ApplicationConfig.Docker.composeDirectory}/docker-compose-$serverId.yml")
            if (composeFile.exists()) {
                composeFile.delete()
            }
            
            activeDeployments.remove(serverId)
            
            logger.info("Deleted Docker game server $serverId")
            true
        } catch (e: Exception) {
            logger.error("Failed to delete Docker game server $serverId", e)
            false
        }
    }
    
    /**
     * Get deployment status
     */
    suspend fun getDeploymentStatus(serverId: String): DockerDeploymentStatus? {
        return try {
            val deployment = activeDeployments[serverId] ?: return null
            
            val runningContainers = getRunningContainers(deployment)
            val totalContainers = deployment.replicas
            
            DockerDeploymentStatus(
                serverId = serverId,
                gameType = deployment.gameType,
                mapId = deployment.mapId,
                playerCount = deployment.playerCount,
                replicas = deployment.replicas,
                runningReplicas = runningContainers.size,
                status = if (runningContainers.size == totalContainers) "Running" else "Degraded",
                containers = runningContainers
            )
        } catch (e: Exception) {
            logger.error("Failed to get deployment status for $serverId", e)
            null
        }
    }
    
    /**
     * Get all deployment statuses
     */
    suspend fun getAllDeploymentStatuses(): List<DockerDeploymentStatus> {
        return activeDeployments.keys.mapNotNull { getDeploymentStatus(it) }
    }
    
    /**
     * Start deployment monitoring
     */
    suspend fun startDeploymentMonitoring() {
        logger.info("Starting Docker deployment monitoring...")
        
        while (scope.isActive) {
            try {
                // Check health of all deployments
                for (deployment in activeDeployments.values) {
                    val status = getDeploymentStatus(deployment.serverId)
                    if (status?.status == "Degraded") {
                        logger.warn("Docker deployment ${deployment.serverId} is degraded")
                    }
                }
                
                delay(30_000) // Check every 30 seconds
            } catch (e: Exception) {
                logger.error("Error during Docker deployment monitoring", e)
                delay(60_000)
            }
        }
    }
    
    /**
     * Create Docker network
     */
    private suspend fun createDockerNetwork() {
        val command = listOf("docker", "network", "create", "--driver", "bridge", networkName)
        val process = ProcessBuilder(command).start()
        val exitCode = process.waitFor()
        
        if (exitCode != 0) {
            // Network might already exist, which is fine
            logger.debug("Docker network $networkName might already exist")
        } else {
            logger.info("Created Docker network: $networkName")
        }
    }
    
    /**
     * Create Docker Compose file using templates
     */
    private fun createDockerComposeFile(deployment: DockerGameDeployment): File {
        val templateName = when (deployment.gameType.id.lowercase()) {
            "bedwars" -> "bedwars-compose.yml"
            "skywars" -> "skywars-compose.yml"
            "duels" -> "duels-compose.yml"
            else -> "bedwars-compose.yml" // Default template
        }
        
        val templateFile = File("src/main/resources/docker-templates/$templateName")
        val templateContent = if (templateFile.exists()) {
            templateFile.readText()
        } else {
            // Fallback to inline template
            createFallbackComposeTemplate(deployment)
        }
        
        // Replace template variables
        val composeContent = templateContent
            .replace("\${CONTAINER_NAME}", deployment.containerName)
            .replace("\${PORT}", deployment.config.port.toString())
            .replace("\${MAP_ID}", deployment.mapId)
            .replace("\${SERVER_ID}", deployment.serverId)
            .replace("\${MAX_PLAYERS}", deployment.playerCount.toString())
            .replace("\${TEAM_SIZE}", deployment.gameType.maxTeamSize.toString())
            .replace("\${DATA_VOLUME}", deployment.config.dataVolume)
            .replace("\${NETWORK_NAME}", networkName)
            .replace("\${CPU_LIMIT}", deployment.config.resourceLimits.cpu)
            .replace("\${MEMORY_LIMIT}", deployment.config.resourceLimits.memory)
            .replace("\${CPU_RESERVATION}", deployment.config.resourceLimits.cpu)
            .replace("\${MEMORY_RESERVATION}", deployment.config.resourceLimits.memory)
        
        val composeFile = File("${ApplicationConfig.Docker.composeDirectory}/docker-compose-${deployment.serverId}.yml")
        composeFile.parentFile.mkdirs()
        composeFile.writeText(composeContent)
        
        return composeFile
    }
    
    /**
     * Create fallback compose template
     */
    private fun createFallbackComposeTemplate(deployment: DockerGameDeployment): String {
        return """
            version: '3.8'
            services:
              game-server:
                image: ${deployment.config.image}
                container_name: ${deployment.containerName}
                ports:
                  - "${deployment.config.port}:25565"
                environment:
                  - GAME_TYPE=${deployment.gameType.id}
                  - MAP_ID=${deployment.mapId}
                  - SERVER_ID=${deployment.serverId}
                  - MAX_PLAYERS=${deployment.playerCount}
                volumes:
                  - ${deployment.config.dataVolume}:/data
                networks:
                  - ${networkName}
                restart: unless-stopped
                deploy:
                  resources:
                    limits:
                      cpus: '${deployment.config.resourceLimits.cpu}'
                      memory: ${deployment.config.resourceLimits.memory}
                    reservations:
                      cpus: '${deployment.config.resourceLimits.cpu}'
                      memory: ${deployment.config.resourceLimits.memory}
                healthcheck:
                  test: ["CMD", "curl", "-f", "http://localhost:25565/health"]
                  interval: 30s
                  timeout: 10s
                  retries: 3
                  start_period: 40s
            
            networks:
              ${networkName}:
                external: true
        """.trimIndent()
    }
    
    /**
     * Start Docker container
     */
    private suspend fun startDockerContainer(
        deployment: DockerGameDeployment, 
        composeFile: File? = null,
        containerName: String? = null
    ) {
        val name = containerName ?: deployment.containerName
        val file = composeFile ?: File("${ApplicationConfig.Docker.composeDirectory}/docker-compose-${deployment.serverId}.yml")
        
        val command = listOf("docker-compose", "-f", file.absolutePath, "up", "-d")
        val process = ProcessBuilder(command).start()
        val exitCode = process.waitFor()
        
        if (exitCode == 0) {
            logger.info("Started Docker container: $name")
        } else {
            throw RuntimeException("Failed to start Docker container: $name")
        }
    }
    
    /**
     * Stop Docker containers
     */
    private suspend fun stopDockerContainers(deployment: DockerGameDeployment) {
        val composeFile = File("${ApplicationConfig.Docker.composeDirectory}/docker-compose-${deployment.serverId}.yml")
        
        val command = listOf("docker-compose", "-f", composeFile.absolutePath, "down")
        val process = ProcessBuilder(command).start()
        val exitCode = process.waitFor()
        
        if (exitCode == 0) {
            logger.info("Stopped Docker containers for ${deployment.serverId}")
        } else {
            logger.warn("Failed to stop Docker containers for ${deployment.serverId}")
        }
    }
    
    /**
     * Remove Docker containers
     */
    private suspend fun removeDockerContainers(deployment: DockerGameDeployment) {
        val command = listOf("docker", "rm", "-f", deployment.containerName)
        val process = ProcessBuilder(command).start()
        val exitCode = process.waitFor()
        
        if (exitCode == 0) {
            logger.info("Removed Docker container: ${deployment.containerName}")
        } else {
            logger.warn("Failed to remove Docker container: ${deployment.containerName}")
        }
    }
    
    /**
     * Get running containers for a deployment
     */
    private suspend fun getRunningContainers(deployment: DockerGameDeployment): List<String> {
        val command = listOf("docker", "ps", "--filter", "name=${deployment.containerName}", "--format", "{{.Names}}")
        val process = ProcessBuilder(command).start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        
        return if (exitCode == 0) {
            output.trim().split("\n").filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
    }
    
    /**
     * Generate unique server ID
     */
    private fun generateServerId(gameType: GameType, mapId: String): String {
        return "${gameType.id}-${mapId}-${System.currentTimeMillis()}"
    }
    
    /**
     * Shutdown the deployment manager
     */
    fun shutdown() {
        logger.info("Shutting down Docker Deployment Manager...")
        scope.coroutineContext.cancelChildren()
        activeDeployments.clear()
        deploymentConfigs.clear()
        logger.info("Docker Deployment Manager shutdown complete")
    }
}

/**
 * Represents a Docker game server deployment
 */
data class DockerGameDeployment(
    val serverId: String,
    val gameType: GameType,
    val mapId: String,
    val playerCount: Int,
    val config: DockerDeploymentConfig,
    val containerName: String,
    val networkName: String,
    var replicas: Int = 1
)

/**
 * Docker deployment configuration
 */
data class DockerDeploymentConfig(
    val image: String,
    val port: Int,
    val dataVolume: String,
    val resourceLimits: ResourceLimits
)

/**
 * Docker deployment status
 */
data class DockerDeploymentStatus(
    val serverId: String,
    val gameType: GameType,
    val mapId: String,
    val playerCount: Int,
    val replicas: Int,
    val runningReplicas: Int,
    val status: String,
    val containers: List<String>
)
