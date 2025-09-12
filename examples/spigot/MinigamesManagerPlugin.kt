package gg.scala.framework.examples.spigot

import gg.scala.framework.application.api.GameServerApi
import gg.scala.framework.application.api.GameServerInfo
import gg.scala.framework.application.api.GameServerStatus
import gg.scala.framework.shared.game.GameType
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

/**
 * Example Spigot plugin that demonstrates how to create and manage game servers
 */
class MinigamesManagerPlugin : JavaPlugin() {
    
    private lateinit var gameServerApi: GameServerApi
    private val pluginScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    override fun onEnable() {
        logger.info("Enabling Minigames Manager Plugin...")
        
        // Initialize the game server API
        // In a real implementation, you'd connect to the minigames manager application
        pluginScope.launch {
            try {
                // This would be initialized with a connection to the minigames manager
                // gameServerApi = GameServerApi(deploymentManager)
                // gameServerApi.initialize()
                
                logger.info("Game Server API initialized successfully")
            } catch (e: Exception) {
                logger.severe("Failed to initialize Game Server API: ${e.message}")
                e.printStackTrace()
            }
        }
        
        // Register commands
        getCommand("creategame")?.setExecutor(this)
        getCommand("destroygame")?.setExecutor(this)
        getCommand("listgames")?.setExecutor(this)
        getCommand("joingame")?.setExecutor(this)
        
        logger.info("Minigames Manager Plugin enabled successfully")
    }
    
    override fun onDisable() {
        logger.info("Disabling Minigames Manager Plugin...")
        
        // Shutdown the API
        pluginScope.launch {
            try {
                // gameServerApi.shutdown()
                pluginScope.coroutineContext.cancelChildren()
            } catch (e: Exception) {
                logger.severe("Error during shutdown: ${e.message}")
            }
        }
        
        logger.info("Minigames Manager Plugin disabled")
    }
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (command.name.lowercase()) {
            "creategame" -> handleCreateGame(sender, args)
            "destroygame" -> handleDestroyGame(sender, args)
            "listgames" -> handleListGames(sender, args)
            "joingame" -> handleJoinGame(sender, args)
            else -> return false
        }
        return true
    }
    
    /**
     * Handle /creategame command
     * Usage: /creategame <gametype> <map> [players]
     */
    private fun handleCreateGame(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("§cUsage: /creategame <gametype> <map> [players]")
            sender.sendMessage("§eGame types: bedwars, skywars, duels")
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
                sender.sendMessage("§cInvalid game type: $gameTypeName")
                return
            }
        }
        
        val creatorId = if (sender is Player) sender.uniqueId.toString() else "console"
        
        // Create the game server asynchronously
        pluginScope.launch {
            try {
                sender.sendMessage("§aCreating game server for ${gameType.displayName} on map $mapId...")
                
                // In a real implementation, you'd call:
                // val serverInfo = gameServerApi.createGameServer(gameType, mapId, playerCount, creatorId)
                
                // For this example, we'll simulate the creation
                val serverInfo = simulateGameServerCreation(gameType, mapId, playerCount, creatorId)
                
                // Schedule a task to run on the main thread
                object : BukkitRunnable() {
                    override fun run() {
                        sender.sendMessage("§aGame server created successfully!")
                        sender.sendMessage("§eServer ID: ${serverInfo.serverId}")
                        sender.sendMessage("§eStatus: ${serverInfo.status}")
                        sender.sendMessage("§ePlayers: ${serverInfo.playerCount}")
                        
                        if (serverInfo.connectionInfo != null) {
                            sender.sendMessage("§eConnection: ${serverInfo.connectionInfo!!.host}:${serverInfo.connectionInfo!!.port}")
                        }
                    }
                }.runTask(this@MinigamesManagerPlugin)
                
            } catch (e: Exception) {
                object : BukkitRunnable() {
                    override fun run() {
                        sender.sendMessage("§cFailed to create game server: ${e.message}")
                    }
                }.runTask(this@MinigamesManagerPlugin)
            }
        }
    }
    
    /**
     * Handle /destroygame command
     * Usage: /destroygame <serverid>
     */
    private fun handleDestroyGame(sender: CommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /destroygame <serverid>")
            return
        }
        
        val serverId = args[0]
        
        pluginScope.launch {
            try {
                sender.sendMessage("§aDestroying game server $serverId...")
                
                // In a real implementation, you'd call:
                // val success = gameServerApi.destroyGameServer(serverId)
                
                // For this example, we'll simulate the destruction
                val success = simulateGameServerDestruction(serverId)
                
                object : BukkitRunnable() {
                    override fun run() {
                        if (success) {
                            sender.sendMessage("§aGame server $serverId destroyed successfully!")
                        } else {
                            sender.sendMessage("§cFailed to destroy game server $serverId")
                        }
                    }
                }.runTask(this@MinigamesManagerPlugin)
                
            } catch (e: Exception) {
                object : BukkitRunnable() {
                    override fun run() {
                        sender.sendMessage("§cFailed to destroy game server: ${e.message}")
                    }
                }.runTask(this@MinigamesManagerPlugin)
            }
        }
    }
    
    /**
     * Handle /listgames command
     * Usage: /listgames [gametype] [status]
     */
    private fun handleListGames(sender: CommandSender, args: Array<out String>) {
        pluginScope.launch {
            try {
                // In a real implementation, you'd call:
                // val servers = gameServerApi.getAllActiveServers()
                
                // For this example, we'll simulate listing servers
                val servers = simulateListGameServers()
                
                object : BukkitRunnable() {
                    override fun run() {
                        sender.sendMessage("§a=== Active Game Servers ===")
                        
                        if (servers.isEmpty()) {
                            sender.sendMessage("§eNo active game servers found.")
                        } else {
                            servers.forEach { server ->
                                val statusColor = when (server.status) {
                                    GameServerStatus.RUNNING -> "§a"
                                    GameServerStatus.CREATING -> "§e"
                                    GameServerStatus.DEGRADED -> "§6"
                                    GameServerStatus.FAILED -> "§c"
                                    GameServerStatus.DESTROYED -> "§7"
                                }
                                
                                sender.sendMessage("§e${server.serverId}")
                                sender.sendMessage("  §7Game: ${server.gameType.displayName}")
                                sender.sendMessage("  §7Map: ${server.mapId}")
                                sender.sendMessage("  §7Players: ${server.playerCount}")
                                sender.sendMessage("  §7Status: $statusColor${server.status}")
                                
                                if (server.connectionInfo != null) {
                                    sender.sendMessage("  §7Connection: ${server.connectionInfo!!.host}:${server.connectionInfo!!.port}")
                                }
                                
                                sender.sendMessage("")
                            }
                        }
                    }
                }.runTask(this@MinigamesManagerPlugin)
                
            } catch (e: Exception) {
                object : BukkitRunnable() {
                    override fun run() {
                        sender.sendMessage("§cFailed to list game servers: ${e.message}")
                    }
                }.runTask(this@MinigamesManagerPlugin)
            }
        }
    }
    
    /**
     * Handle /joingame command
     * Usage: /joingame <serverid>
     */
    private fun handleJoinGame(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can join games!")
            return
        }
        
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /joingame <serverid>")
            return
        }
        
        val serverId = args[0]
        
        pluginScope.launch {
            try {
                // In a real implementation, you'd call:
                // val serverInfo = gameServerApi.getServerInfo(serverId)
                
                // For this example, we'll simulate getting server info
                val serverInfo = simulateGetServerInfo(serverId)
                
                object : BukkitRunnable() {
                    override fun run() {
                        if (serverInfo == null) {
                            sender.sendMessage("§cGame server $serverId not found!")
                            return
                        }
                        
                        if (serverInfo.status != GameServerStatus.RUNNING) {
                            sender.sendMessage("§cGame server $serverId is not running (Status: ${serverInfo.status})")
                            return
                        }
                        
                        if (serverInfo.connectionInfo == null) {
                            sender.sendMessage("§cGame server $serverId has no connection information!")
                            return
                        }
                        
                        // In a real implementation, you'd connect the player to the server
                        // This could be done through BungeeCord/Velocity or direct connection
                        sender.sendMessage("§aConnecting to game server $serverId...")
                        sender.sendMessage("§eServer: ${serverInfo.gameType.displayName}")
                        sender.sendMessage("§eMap: ${serverInfo.mapId}")
                        sender.sendMessage("§eConnection: ${serverInfo.connectionInfo!!.host}:${serverInfo.connectionInfo!!.port}")
                        
                        // Simulate connection (in real implementation, you'd use BungeeCord/Velocity API)
                        // BungeeCordAPI.connect(sender, serverInfo.connectionInfo!!.host, serverInfo.connectionInfo!!.port)
                    }
                }.runTask(this@MinigamesManagerPlugin)
                
            } catch (e: Exception) {
                object : BukkitRunnable() {
                    override fun run() {
                        sender.sendMessage("§cFailed to join game server: ${e.message}")
                    }
                }.runTask(this@MinigamesManagerPlugin)
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
    
    private suspend fun simulateListGameServers(): List<GameServerInfo> {
        delay(500) // Simulate API call time
        
        return listOf(
            GameServerInfo(
                serverId = "bedwars-map1-1234567890",
                gameType = GameType("bedwars", "BedWars", "cgs-bedwars"),
                mapId = "map1",
                playerCount = 16,
                creatorId = "player1",
                status = GameServerStatus.RUNNING,
                createdAt = System.currentTimeMillis(),
                connectionInfo = gg.scala.framework.application.api.ConnectionInfo(
                    host = "localhost",
                    port = 25566,
                    protocol = "TCP"
                )
            ),
            GameServerInfo(
                serverId = "skywars-map2-1234567891",
                gameType = GameType("skywars", "SkyWars", "cgs-skywars"),
                mapId = "map2",
                playerCount = 12,
                creatorId = "player2",
                status = GameServerStatus.CREATING,
                createdAt = System.currentTimeMillis()
            )
        )
    }
    
    private suspend fun simulateGetServerInfo(serverId: String): GameServerInfo? {
        delay(300) // Simulate API call time
        
        return if (serverId.startsWith("bedwars")) {
            GameServerInfo(
                serverId = serverId,
                gameType = GameType("bedwars", "BedWars", "cgs-bedwars"),
                mapId = "map1",
                playerCount = 16,
                creatorId = "player1",
                status = GameServerStatus.RUNNING,
                createdAt = System.currentTimeMillis(),
                connectionInfo = gg.scala.framework.application.api.ConnectionInfo(
                    host = "localhost",
                    port = 25566,
                    protocol = "TCP"
                )
            )
        } else {
            null
        }
    }
}
