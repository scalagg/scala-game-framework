package gg.scala.framework.application

import gg.scala.framework.application.deployment.DockerDeploymentConfig
import gg.scala.framework.application.game.GameType
import gg.scala.framework.application.game.ResourceLimits as GameResourceLimits
import gg.scala.framework.application.game.PortRange
import gg.scala.framework.application.game.ScalingConfig
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Main entry point for the Minigames Manager Application
 */
fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("MinigamesManager")
    
    runBlocking {
        val application = MinigamesManagerApplication()
        
        try {
            // Initialize the application
            application.initialize()
            
            // Configure game types
            configureGameTypes(application)
            
            // Configure deployment strategies
            configureDeploymentStrategies(application)
            
            logger.info("Minigames Manager Application started successfully")
            
            // Keep the application running
            // In a real deployment, this would be handled by the container orchestrator
            while (true) {
                kotlinx.coroutines.delay(1000)
            }
            
        } catch (e: Exception) {
            logger.error("Failed to start Minigames Manager Application", e)
            System.exit(1)
        } finally {
            application.shutdown()
        }
    }
}

/**
 * Configure deployment strategies for different game types
 */
suspend fun configureDeploymentStrategies(application: MinigamesManagerApplication) {
    val logger = LoggerFactory.getLogger("DeploymentConfig")
    
    // Get deployment strategy from configuration
    val strategy = gg.scala.framework.application.config.ApplicationConfig.Deployment.strategy
    logger.info("Configuring deployment strategies with: $strategy")
    
    when (strategy.lowercase()) {
        "kubernetes", "k8s" -> {
            logger.info("Using Kubernetes deployment strategy")
        }
        "docker" -> {
            logger.info("Using Docker deployment strategy")
        }
        else -> {
            logger.warn("Unknown deployment strategy: $strategy, defaulting to Docker")
        }
    }
    
    logger.info("Deployment strategies configured successfully")
}

/**
 * Configure game types with their settings
 */
private suspend fun configureGameTypes(application: MinigamesManagerApplication) {
    // Configure BedWars
    val bedWarsGameType = GameType(
        id = "bedwars",
        displayName = "BedWars",
        dataStore = "cgs-bedwars",
        allowTeams = true,
        maxTeamSize = 4,
        minTeamSize = 1,
        enabled = true,
        resourceLimits = GameResourceLimits("1", "2Gi", "3Gi"),
        portRange = PortRange(25565, 25585),
        scalingConfig = ScalingConfig(
            minInstances = 2,
            maxInstances = 20,
            scaleUpThreshold = 0.8,
            scaleDownThreshold = 0.3
        )
    )
    
    application.configureGameType(
        bedWarsGameType,
        GameTypeConfig(
            maxPlayers = 16,
            minPlayers = 8,
            teamSize = 4,
            kitRequired = false,
            matchmakingSettings = MatchmakingSettings(
                maxPing = 150,
                eloRange = 200,
                allowCrossRegion = false
            ),
            deploymentSettings = DeploymentSettings(
                maxInstances = 20,
                autoScale = true,
                resourceLimits = ResourceLimits(
                    cpu = "1000m",
                    memory = "2Gi",
                    storage = "4Gi"
                )
            )
        )
    )
    
    // Configure SkyWars
    val skyWarsGameType = GameType(
        id = "skywars",
        displayName = "SkyWars",
        dataStore = "cgs-skywars",
        allowTeams = false,
        maxTeamSize = 1,
        minTeamSize = 1,
        enabled = true,
        resourceLimits = GameResourceLimits("750m", "1.5Gi", "2Gi"),
        portRange = PortRange(25585, 25605),
        scalingConfig = ScalingConfig(
            minInstances = 1,
            maxInstances = 15,
            scaleUpThreshold = 0.7,
            scaleDownThreshold = 0.2
        )
    )
    
    application.configureGameType(
        skyWarsGameType,
        GameTypeConfig(
            maxPlayers = 12,
            minPlayers = 6,
            teamSize = 1,
            kitRequired = true,
            matchmakingSettings = MatchmakingSettings(
                maxPing = 200,
                eloRange = 150,
                allowCrossRegion = true
            ),
            deploymentSettings = DeploymentSettings(
                maxInstances = 15,
                autoScale = true,
                resourceLimits = ResourceLimits(
                    cpu = "750m",
                    memory = "1.5Gi",
                    storage = "3Gi"
                )
            )
        )
    )
    
    // Configure Duels
    val duelsGameType = GameType(
        id = "duels",
        displayName = "Duels",
        dataStore = "cgs-duels",
        allowTeams = false,
        maxTeamSize = 1,
        minTeamSize = 1,
        enabled = true,
        resourceLimits = GameResourceLimits("250m", "512Mi", "1Gi"),
        portRange = PortRange(25605, 25625),
        scalingConfig = ScalingConfig(
            minInstances = 1,
            maxInstances = 10,
            scaleUpThreshold = 0.9,
            scaleDownThreshold = 0.1
        )
    )
    
    application.configureGameType(
        duelsGameType,
        GameTypeConfig(
            maxPlayers = 2,
            minPlayers = 2,
            teamSize = 1,
            kitRequired = true,
            matchmakingSettings = MatchmakingSettings(
                maxPing = 100,
                eloRange = 100,
                allowCrossRegion = false
            ),
            deploymentSettings = DeploymentSettings(
                maxInstances = 50,
                autoScale = true,
                resourceLimits = ResourceLimits(
                    cpu = "250m",
                    memory = "512Mi",
                    storage = "1Gi"
                )
            )
        )
    )
}
