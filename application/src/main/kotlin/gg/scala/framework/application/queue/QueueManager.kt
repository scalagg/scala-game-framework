package gg.scala.framework.application.queue

import gg.scala.framework.shared.game.GameType
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

/**
 * Manages player queues for matchmaking across different game types, team sizes, and kits.
 * Each queue is independent and processes players based on matchmaking constraints.
 */
class QueueManager {
    
    private val logger = LoggerFactory.getLogger(QueueManager::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Queue storage: GameType -> TeamSize -> Kit -> Queue
    private val queues = ConcurrentHashMap<GameType, ConcurrentHashMap<Int, ConcurrentHashMap<String, QueueEntry>>>()
    
    // Active matchmaking processes
    private val activeMatchmaking = ConcurrentHashMap<String, Job>()
    
    /**
     * Initialize the queue manager
     */
    suspend fun initialize() {
        logger.info("Initializing Queue Manager...")
        // Initialize any required resources here
        logger.info("Queue Manager initialized")
    }
    
    /**
     * Add a player to the queue
     */
    fun addPlayerToQueue(
        playerId: UUID,
        gameType: GameType,
        teamSize: Int,
        kit: String,
        playerData: PlayerData
    ) {
        val queue = getOrCreateQueue(gameType, teamSize, kit)
        queue.addPlayer(playerData)
        
        logger.debug("Player $playerId added to queue: $gameType-$teamSize-$kit")
        
        // Trigger matchmaking if we have enough players
        triggerMatchmaking(gameType, teamSize, kit)
    }
    
    /**
     * Remove a player from the queue
     */
    fun removePlayerFromQueue(
        playerId: UUID,
        gameType: GameType,
        teamSize: Int,
        kit: String
    ) {
        val queue = queues[gameType]?.get(teamSize)?.get(kit)
        queue?.removePlayer(playerId)
        
        logger.debug("Player $playerId removed from queue: $gameType-$teamSize-$kit")
    }
    
    /**
     * Get queue statistics
     */
    fun getQueueStats(gameType: GameType, teamSize: Int, kit: String): QueueStats {
        val queue = queues[gameType]?.get(teamSize)?.get(kit)
        return queue?.getStats() ?: QueueStats(0, 0, 0)
    }
    
    /**
     * Shutdown the queue manager
     */
    fun shutdown() {
        logger.info("Shutting down Queue Manager...")
        scope.coroutineContext.cancelChildren()
        queues.clear()
        activeMatchmaking.clear()
        logger.info("Queue Manager shutdown complete")
    }
    
    /**
     * Start queue processing
     */
    suspend fun startQueueProcessing() {
        logger.info("Starting queue processing...")
        
        // Process queues every 100ms
        while (scope.isActive) {
            try {
                processAllQueues()
                delay(100)
            } catch (e: Exception) {
                logger.error("Error processing queues", e)
                delay(1000) // Wait longer on error
            }
        }
    }
    
    /**
     * Process all active queues
     */
    private suspend fun processAllQueues() {
        for ((gameType, teamSizeMap) in queues) {
            for ((teamSize, kitMap) in teamSizeMap) {
                for ((kit, queue) in kitMap) {
                    if (queue.canCreateMatch()) {
                        triggerMatchmaking(gameType, teamSize, kit)
                    }
                }
            }
        }
    }
    
    /**
     * Trigger matchmaking for a specific queue
     */
    private fun triggerMatchmaking(gameType: GameType, teamSize: Int, kit: String) {
        val queueKey = "$gameType-$teamSize-$kit"
        
        // Cancel existing matchmaking if running
        activeMatchmaking[queueKey]?.cancel()
        
        // Start new matchmaking
        activeMatchmaking[queueKey] = scope.launch {
            try {
                val queue = queues[gameType]?.get(teamSize)?.get(kit) ?: return@launch
                
                if (queue.canCreateMatch()) {
                    val match = queue.createMatch()
                    if (match != null) {
                        logger.info("Created match for $queueKey with ${match.players.size} players")
                        // TODO: Deploy game server and assign players
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in matchmaking for $queueKey", e)
            } finally {
                activeMatchmaking.remove(queueKey)
            }
        }
    }
    
    /**
     * Get or create a queue for the given parameters
     */
    private fun getOrCreateQueue(gameType: GameType, teamSize: Int, kit: String): QueueEntry {
        return queues
            .computeIfAbsent(gameType) { ConcurrentHashMap() }
            .computeIfAbsent(teamSize) { ConcurrentHashMap() }
            .computeIfAbsent(kit) { QueueEntry(gameType, teamSize, kit) }
    }
}

/**
 * Represents a queue entry for a specific game type, team size, and kit combination
 */
class QueueEntry(
    private val gameType: GameType,
    private val teamSize: Int,
    private val kit: String
) {
    private val players = ConcurrentLinkedQueue<PlayerData>()
    private val maxPlayers = teamSize * 8 // Assume max 8 teams per game
    
    /**
     * Add a player to this queue
     */
    fun addPlayer(playerData: PlayerData) {
        players.offer(playerData)
    }
    
    /**
     * Remove a player from this queue
     */
    fun removePlayer(playerId: UUID) {
        players.removeIf { it.playerId == playerId }
    }
    
    /**
     * Check if we can create a match
     */
    fun canCreateMatch(): Boolean {
        return players.size >= teamSize * 2 // Minimum 2 teams
    }
    
    /**
     * Create a match from available players
     */
    fun createMatch(): Match? {
        if (!canCreateMatch()) return null
        
        val availablePlayers = players.toList()
        val matchPlayers = mutableListOf<PlayerData>()
        
        // Simple matchmaking: take first available players
        // TODO: Implement proper matchmaking with ELO, ping, etc.
        repeat(minOf(maxPlayers, availablePlayers.size)) {
            val player = availablePlayers[it]
            matchPlayers.add(player)
            players.remove(player)
        }
        
        return Match(
            id = UUID.randomUUID(),
            gameType = gameType,
            teamSize = teamSize,
            kit = kit,
            players = matchPlayers
        )
    }
    
    /**
     * Get queue statistics
     */
    fun getStats(): QueueStats {
        return QueueStats(
            playerCount = players.size,
            maxPlayers = maxPlayers,
            estimatedWaitTime = calculateEstimatedWaitTime()
        )
    }
    
    /**
     * Calculate estimated wait time based on current queue size
     */
    private fun calculateEstimatedWaitTime(): Long {
        val currentSize = players.size
        val neededForMatch = teamSize * 2
        
        if (currentSize < neededForMatch) {
            // Estimate based on historical data
            return (neededForMatch - currentSize) * 30L // 30 seconds per player
        }
        
        return 0L // Ready for match
    }
}

/**
 * Player data for matchmaking
 */
data class PlayerData(
    val playerId: UUID,
    val username: String,
    val elo: Int = 1000,
    val ping: Int = 0,
    val region: String = "US",
    val joinedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a match created from the queue
 */
data class Match(
    val id: UUID,
    val gameType: GameType,
    val teamSize: Int,
    val kit: String,
    val players: List<PlayerData>,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Queue statistics
 */
data class QueueStats(
    val playerCount: Int,
    val maxPlayers: Int,
    val estimatedWaitTime: Long
)
