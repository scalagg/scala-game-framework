package gg.scala.framework.examples.velocity

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import gg.scala.framework.application.api.GameServerApi
import gg.scala.framework.application.api.GameServerInfo
import gg.scala.framework.application.api.GameServerStatus
import gg.scala.framework.application.game.GameType
import kotlinx.coroutines.*
import org.slf4j.Logger
import java.util.*

/**
 * Example Velocity plugin that demonstrates how to create and manage game servers
 */
@Plugin(
    id = "minigames-manager",
    name = "Minigames Manager",
    version = "1.0.0",
    description = "Manages minigame servers",
    authors = ["YourName"]
)
class MinigamesManagerVelocityPlugin {
    
    private lateinit var server: ProxyServer
    private lateinit var logger: Logger
    private lateinit var gameServerApi: GameServerApi
    private val pluginScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Track game servers and their corresponding Velocity servers
    private val gameServers = mutableMapOf<String, GameServerInfo>()
    private val velocityServers = mutableMapOf<String, RegisteredServer>()
    
    fun onEnable(server: ProxyServer) {
        this.server = server
        this.logger = server.logger
        
        logger.info("Enabling Minigames Manager Velocity Plugin...")
        
        // Initialize the game server API
        pluginScope.launch {
            try {
                // This would be initialized with a connection to the minigames manager
                // gameServerApi = GameServerApi(deploymentManager)
                // gameServerApi.initialize()
                
                logger.info("Game Server API initialized successfully")
            } catch (e: Exception) {
                logger.error("Failed to initialize Game Server API: ${e.message}", e)
            }
        }
        
        // Register commands
        server.commandManager.register("creategame", CreateGameCommand())
        server.commandManager.register("destroygame", DestroyGameCommand())
        server.commandManager.register("listgames", ListGamesCommand())
        server.commandManager.register("joingame", JoinGameCommand())
        
        // Register event listeners
        server.eventManager.register(this, this)
        
        logger.info("Minigames Manager Velocity Plugin enabled successfully")
    }
    
    fun onDisable() {
        logger.info("Disabling Minigames Manager Velocity Plugin...")
        
        // Shutdown the API
        pluginScope.launch {
            try {
                // gameServerApi.shutdown()
                pluginScope.coroutineContext.cancelChildren()
            } catch (e: Exception) {
                logger.error("Error during shutdown: ${e.message}", e)
            }
        }
        
        logger.info("Minigames Manager Velocity Plugin disabled")
    }
    
    /**
     * Create a new game server
     */
    suspend fun createGameServer(
        gameType: GameType,
        mapId: String,
        playerCount: Int,
        creatorId: String
    ): GameServerInfo {
        logger.info("Creating game server for ${gameType.displayName} on map $mapId")
        
        try {
            // In a real implementation, you'd call:
            // val serverInfo = gameServerApi.createGameServer(gameType, mapId, playerCount, creatorId)
            
            // For this example, we'll simulate the creation
            val serverInfo = simulateGameServerCreation(gameType, mapId, playerCount, creatorId)
            
            // Register the server with Velocity
            val velocityServer = registerVelocityServer(serverInfo)
            velocityServers[serverInfo.serverId] = velocityServer
            gameServers[serverInfo.serverId] = serverInfo
            
            logger.info("Game server ${serverInfo.serverId} created and registered with Velocity")
            return serverInfo
            
        } catch (e: Exception) {
            logger.error("Failed to create game server", e)
            throw e
        }
    }
    
    /**
     * Destroy a game server
     */
    suspend fun destroyGameServer(serverId: String): Boolean {
        logger.info("Destroying game server: $serverId")
        
        try {
            // In a real implementation, you'd call:
            // val success = gameServerApi.destroyGameServer(serverId)
            
            // For this example, we'll simulate the destruction
            val success = simulateGameServerDestruction(serverId)
            
            if (success) {
                // Unregister from Velocity
                val velocityServer = velocityServers.remove(serverId)
                if (velocityServer != null) {
                    server.unregisterServer(velocityServer.serverInfo)
                }
                
                // Remove from tracking
                gameServers.remove(serverId)
                
                logger.info("Game server $serverId destroyed and unregistered from Velocity")
            }
            
            return success
            
        } catch (e: Exception) {
            logger.error("Failed to destroy game server $serverId", e)
            return false
        }
    }
    
    /**
     * Register a game server with Velocity
     */
    private fun registerVelocityServer(serverInfo: GameServerInfo): RegisteredServer {
        val serverInfo = com.velocitypowered.api.proxy.server.ServerInfo(
            serverInfo.serverId,
            java.net.InetSocketAddress(
                serverInfo.connectionInfo!!.host,
                serverInfo.connectionInfo!!.port
            )
        )
        
        return server.registerServer(serverInfo)
    }
    
    /**
     * Get server information
     */
    fun getServerInfo(serverId: String): GameServerInfo? {
        return gameServers[serverId]
    }
    
    /**
     * Get all active servers
     */
    fun getAllActiveServers(): List<GameServerInfo> {
        return gameServers.values.toList()
    }
    
    /**
     * Connect a player to a game server
     */
    fun connectPlayerToGame(player: Player, serverId: String): Boolean {
        val serverInfo = gameServers[serverId]
        if (serverInfo == null) {
            player.sendMessage("§cGame server $serverId not found!")
            return false
        }
        
        if (serverInfo.status != GameServerStatus.RUNNING) {
            player.sendMessage("§cGame server $serverId is not running (Status: ${serverInfo.status})")
            return false
        }
        
        val velocityServer = velocityServers[serverId]
        if (velocityServer == null) {
            player.sendMessage("§cGame server $serverId is not registered with Velocity!")
            return false
        }
        
        // Connect the player to the server
        player.createConnectionRequest(velocityServer).fireAndForget()
        
        player.sendMessage("§aConnecting to game server $serverId...")
        player.sendMessage("§eServer: ${serverInfo.gameType.displayName}")
        player.sendMessage("§eMap: ${serverInfo.mapId}")
        
        return true
    }
    
    /**
     * Handle server connection events
     */
    @Subscribe
    fun onServerConnected(event: ServerConnectedEvent) {
        val serverId = event.server.serverInfo.name
        val gameServer = gameServers[serverId]
        
        if (gameServer != null) {
            logger.info("Player ${event.player.username} connected to game server $serverId")
            
            // Update player count or perform other actions
            // This could trigger matchmaking logic, etc.
        }
    }
    
    /**
     * Handle plugin messages (for communication with game servers)
     */
    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.identifier.id == "minigames:status") {
            // Handle status updates from game servers
            val serverId = event.source.toString()
            val gameServer = gameServers[serverId]
            
            if (gameServer != null) {
                // Process status update
                logger.debug("Received status update from game server $serverId")
            }
        }
    }
    
    // Command implementations
    inner class CreateGameCommand : SimpleCommand {
        override fun execute(invocation: SimpleCommand.Invocation) {
            val source = invocation.source()
            val args = invocation.arguments()
            
            if (args.size < 2) {
                source.sendMessage("§cUsage: /creategame <gametype> <map> [players]")
                source.sendMessage("§eGame types: bedwars, skywars, duels")
                return
            }
            
            val gameTypeName = args[0].lowercase()
            val mapId = args[1]
            val playerCount = if (args.size > 2) args[2].toIntOrNull() ?: 16 else 16
            
            // Validate game type
            val gameType = when (gameTypeName) {
                "bedwars" -> GameType("bedwars", "BedWars", "cgs-bedwars")
                "skywars" -> GameType("skywars", "SkyWars", "cgs-skywars")
                "duels" -> GameType("duels", "Duels", "cgs-duels")
                else -> {
                    source.sendMessage("§cInvalid game type: $gameTypeName")
                    return
                }
            }
            
            val creatorId = if (source is Player) source.uniqueId.toString() else "console"
            
            // Create the game server asynchronously
            pluginScope.launch {
                try {
                    source.sendMessage("§aCreating game server for ${gameType.displayName} on map $mapId...")
                    
                    val serverInfo = createGameServer(gameType, mapId, playerCount, creatorId)
                    
                    source.sendMessage("§aGame server created successfully!")
                    source.sendMessage("§eServer ID: ${serverInfo.serverId}")
                    source.sendMessage("§eStatus: ${serverInfo.status}")
                    source.sendMessage("§ePlayers: ${serverInfo.playerCount}")
                    
                    if (serverInfo.connectionInfo != null) {
                        source.sendMessage("§eConnection: ${serverInfo.connectionInfo!!.host}:${serverInfo.connectionInfo!!.port}")
                    }
                    
                } catch (e: Exception) {
                    source.sendMessage("§cFailed to create game server: ${e.message}")
                }
            }
        }
    }
    
    inner class DestroyGameCommand : SimpleCommand {
        override fun execute(invocation: SimpleCommand.Invocation) {
            val source = invocation.source()
            val args = invocation.arguments()
            
            if (args.isEmpty()) {
                source.sendMessage("§cUsage: /destroygame <serverid>")
                return
            }
            
            val serverId = args[0]
            
            pluginScope.launch {
                try {
                    source.sendMessage("§aDestroying game server $serverId...")
                    
                    val success = destroyGameServer(serverId)
                    
                    if (success) {
                        source.sendMessage("§aGame server $serverId destroyed successfully!")
                    } else {
                        source.sendMessage("§cFailed to destroy game server $serverId")
                    }
                    
                } catch (e: Exception) {
                    source.sendMessage("§cFailed to destroy game server: ${e.message}")
                }
            }
        }
    }
    
    inner class ListGamesCommand : SimpleCommand {
        override fun execute(invocation: SimpleCommand.Invocation) {
            val source = invocation.source()
            
            pluginScope.launch {
                try {
                    val servers = getAllActiveServers()
                    
                    source.sendMessage("§a=== Active Game Servers ===")
                    
                    if (servers.isEmpty()) {
                        source.sendMessage("§eNo active game servers found.")
                    } else {
                        servers.forEach { server ->
                            val statusColor = when (server.status) {
                                GameServerStatus.RUNNING -> "§a"
                                GameServerStatus.CREATING -> "§e"
                                GameServerStatus.DEGRADED -> "§6"
                                GameServerStatus.FAILED -> "§c"
                                GameServerStatus.DESTROYED -> "§7"
                            }
                            
                            source.sendMessage("§e${server.serverId}")
                            source.sendMessage("  §7Game: ${server.gameType.displayName}")
                            source.sendMessage("  §7Map: ${server.mapId}")
                            source.sendMessage("  §7Players: ${server.playerCount}")
                            source.sendMessage("  §7Status: $statusColor${server.status}")
                            
                            if (server.connectionInfo != null) {
                                source.sendMessage("  §7Connection: ${server.connectionInfo!!.host}:${server.connectionInfo!!.port}")
                            }
                            
                            source.sendMessage("")
                        }
                    }
                    
                } catch (e: Exception) {
                    source.sendMessage("§cFailed to list game servers: ${e.message}")
                }
            }
        }
    }
    
    inner class JoinGameCommand : SimpleCommand {
        override fun execute(invocation: SimpleCommand.Invocation) {
            val source = invocation.source()
            val args = invocation.arguments()
            
            if (source !is Player) {
                source.sendMessage("§cOnly players can join games!")
                return
            }
            
            if (args.isEmpty()) {
                source.sendMessage("§cUsage: /joingame <serverid>")
                return
            }
            
            val serverId = args[0]
            
            pluginScope.launch {
                try {
                    val success = connectPlayerToGame(source, serverId)
                    
                    if (!success) {
                        source.sendMessage("§cFailed to join game server $serverId")
                    }
                    
                } catch (e: Exception) {
                    source.sendMessage("§cFailed to join game server: ${e.message}")
                }
            }
        }
    }
    
    // Simulation methods for demonstration purposes
    private suspend fun simulateGameServerCreation(
        gameType: GameType,
        mapId: String,
        playerCount: Int,
        creatorId: String
    ): GameServerInfo {
        delay(2000) // Simulate creation time
        
        return GameServerInfo(
            serverId = "${gameType.id}-${mapId}-${System.currentTimeMillis()}",
            gameType = gameType,
            mapId = mapId,
            playerCount = playerCount,
            creatorId = creatorId,
            status = GameServerStatus.RUNNING,
            createdAt = System.currentTimeMillis(),
            connectionInfo = gg.scala.framework.application.api.ConnectionInfo(
                host = "localhost",
                port = 25565 + (Math.random() * 100).toInt(),
                protocol = "TCP"
            )
        )
    }
    
    private suspend fun simulateGameServerDestruction(serverId: String): Boolean {
        delay(1000) // Simulate destruction time
        return true
    }
}
