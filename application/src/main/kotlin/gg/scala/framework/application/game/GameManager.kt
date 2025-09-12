package gg.scala.framework.application.game

import gg.scala.framework.shared.game.GameType
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages ongoing games and their statuses for each game server.
 * Games are invalidated if no status update is received within 2 seconds.
 */
class GameManager {
    
    private val logger = LoggerFactory.getLogger(GameManager::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Active games by game ID
    private val activeGames = ConcurrentHashMap<UUID, GameInstance>()
    
    // Games by server ID
    private val gamesByServer = ConcurrentHashMap<String, MutableSet<UUID>>()
    
    // Game status update timeout (2 seconds)
    private val statusTimeout = 2000L
    
    /**
     * Initialize the game manager
     */
    suspend fun initialize() {
        logger.info("Initializing Game Manager...")
        // Initialize any required resources here
        logger.info("Game Manager initialized")
    }
    
    /**
     * Register a new game instance
     */
    fun registerGame(gameInstance: GameInstance) {
        activeGames[gameInstance.id] = gameInstance
        gamesByServer.computeIfAbsent(gameInstance.serverId) { mutableSetOf() }.add(gameInstance.id)
        
        logger.info("Registered game ${gameInstance.id} on server ${gameInstance.serverId}")
    }
    
    /**
     * Update game status
     */
    fun updateGameStatus(gameId: UUID, status: GameStatus) {
        val game = activeGames[gameId]
        if (game != null) {
            game.updateStatus(status)
            logger.debug("Updated status for game $gameId: ${status.state}")
        } else {
            logger.warn("Attempted to update status for unknown game $gameId")
        }
    }
    
    /**
     * Get game instance
     */
    fun getGame(gameId: UUID): GameInstance? = activeGames[gameId]
    
    /**
     * Get all games for a server
     */
    fun getGamesForServer(serverId: String): List<GameInstance> {
        return gamesByServer[serverId]?.mapNotNull { gameId ->
            activeGames[gameId]
        } ?: emptyList()
    }
    
    /**
     * Get all active games
     */
    fun getAllActiveGames(): List<GameInstance> = activeGames.values.toList()
    
    /**
     * Get games by game type
     */
    fun getGamesByType(gameType: GameType): List<GameInstance> {
        return activeGames.values.filter { it.gameType == gameType }
    }
    
    /**
     * Get games by state
     */
    fun getGamesByState(state: GameState): List<GameInstance> {
        return activeGames.values.filter { it.status.state == state }
    }
    
    /**
     * Remove a game instance
     */
    fun removeGame(gameId: UUID) {
        val game = activeGames.remove(gameId)
        if (game != null) {
            gamesByServer[game.serverId]?.remove(gameId)
            logger.info("Removed game $gameId from server ${game.serverId}")
        }
    }
    
    /**
     * Start game monitoring
     */
    suspend fun startGameMonitoring() {
        logger.info("Starting game monitoring...")
        
        // Check for stale games every 1 second
        while (scope.isActive) {
            try {
                checkForStaleGames()
                delay(1000)
            } catch (e: Exception) {
                logger.error("Error monitoring games", e)
                delay(5000) // Wait longer on error
            }
        }
    }
    
    /**
     * Check for games that haven't updated their status recently
     */
    private fun checkForStaleGames() {
        val currentTime = System.currentTimeMillis()
        val staleGames = mutableListOf<UUID>()
        
        for ((gameId, game) in activeGames) {
            val timeSinceLastUpdate = currentTime - game.lastStatusUpdate
            if (timeSinceLastUpdate > statusTimeout) {
                staleGames.add(gameId)
                logger.warn("Game $gameId on server ${game.serverId} is stale (${timeSinceLastUpdate}ms since last update)")
            }
        }
        
        // Remove stale games
        for (gameId in staleGames) {
            val game = activeGames[gameId]
            if (game != null) {
                logger.warn("Removing stale game $gameId from server ${game.serverId}")
                removeGame(gameId)
                
                // TODO: Notify deployment manager to restart server if needed
            }
        }
    }
    
    /**
     * Get game statistics
     */
    fun getGameStatistics(): GameStatistics {
        val totalGames = activeGames.size
        val gamesByState = activeGames.values.groupBy { it.status.state }
        val gamesByType = activeGames.values.groupBy { it.gameType }
        
        return GameStatistics(
            totalGames = totalGames,
            gamesByState = gamesByState.mapValues { it.value.size },
            gamesByType = gamesByType.mapValues { it.value.size },
            averagePlayersPerGame = if (totalGames > 0) {
                activeGames.values.sumOf { it.status.playerCount } / totalGames
            } else 0
        )
    }
    
    /**
     * Shutdown the game manager
     */
    fun shutdown() {
        logger.info("Shutting down Game Manager...")
        scope.coroutineContext.cancelChildren()
        activeGames.clear()
        gamesByServer.clear()
        logger.info("Game Manager shutdown complete")
    }
}

/**
 * Represents a game instance running on a server
 */
class GameInstance(
    val id: UUID,
    val gameType: GameType,
    val serverId: String,
    val mapId: String,
    val players: List<UUID>,
    val createdAt: Long = System.currentTimeMillis()
) {
    private var _status: GameStatus = GameStatus(GameState.WAITING, players.size, 0)
    var lastStatusUpdate: Long = System.currentTimeMillis()
        private set
    
    val status: GameStatus get() = _status
    
    fun updateStatus(newStatus: GameStatus) {
        _status = newStatus
        lastStatusUpdate = System.currentTimeMillis()
    }
}

/**
 * Game status information
 */
data class GameStatus(
    val state: GameState,
    val playerCount: Int,
    val maxPlayers: Int,
    val gameTime: Long = 0L,
    val additionalData: Map<String, Any> = emptyMap()
)

/**
 * Game states
 */
enum class GameState {
    WAITING,        // Waiting for players
    STARTING,       // Game is starting
    IN_PROGRESS,    // Game is running
    ENDING,         // Game is ending
    FINISHED        // Game has finished
}

/**
 * Game statistics
 */
data class GameStatistics(
    val totalGames: Int,
    val gamesByState: Map<GameState, Int>,
    val gamesByType: Map<GameType, Int>,
    val averagePlayersPerGame: Int
)
