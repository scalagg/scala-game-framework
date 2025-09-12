package gg.scala.framework.application.metrics

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages application metrics and monitoring
 */
class MetricsManager {
    
    private val logger = LoggerFactory.getLogger(MetricsManager::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Counters
    private val counters = ConcurrentHashMap<String, AtomicLong>()
    
    // Gauges
    private val gauges = ConcurrentHashMap<String, () -> Number>()
    
    // Timers
    private val timers = ConcurrentHashMap<String, MutableList<Long>>()
    
    /**
     * Initialize metrics manager
     */
    suspend fun initialize() {
        logger.info("Initializing Metrics Manager...")
        
        // Register default metrics
        registerCounter("games.created")
        registerCounter("games.completed")
        registerCounter("games.failed")
        registerCounter("players.queued")
        registerCounter("players.matched")
        registerCounter("servers.deployed")
        registerCounter("servers.deleted")
        registerCounter("replications.created")
        registerCounter("replications.destroyed")
        
        // Register default gauges
        registerGauge("games.active") { 0L }
        registerGauge("players.in_queue") { 0L }
        registerGauge("servers.running") { 0L }
        registerGauge("replications.available") { 0L }
        
        logger.info("Metrics Manager initialized")
    }
    
    /**
     * Register a counter metric
     */
    fun registerCounter(name: String) {
        counters[name] = AtomicLong(0)
    }
    
    /**
     * Increment a counter
     */
    fun incrementCounter(name: String, value: Long = 1) {
        counters[name]?.addAndGet(value)
    }
    
    /**
     * Get counter value
     */
    fun getCounter(name: String): Long {
        return counters[name]?.get() ?: 0L
    }
    
    /**
     * Register a gauge metric
     */
    fun registerGauge(name: String, gauge: () -> Number) {
        gauges[name] = gauge
    }
    
    /**
     * Get gauge value
     */
    fun getGauge(name: String): Number? {
        return gauges[name]?.invoke()
    }
    
    /**
     * Record a timer value
     */
    fun recordTimer(name: String, value: Long) {
        timers.computeIfAbsent(name) { mutableListOf() }.add(value)
    }
    
    /**
     * Get timer statistics
     */
    fun getTimerStats(name: String): TimerStats? {
        val values = timers[name] ?: return null
        if (values.isEmpty()) return null
        
        val sorted = values.sorted()
        return TimerStats(
            count = values.size,
            min = sorted.first(),
            max = sorted.last(),
            avg = values.average(),
            p50 = sorted[sorted.size / 2],
            p95 = sorted[(sorted.size * 0.95).toInt()],
            p99 = sorted[(sorted.size * 0.99).toInt()]
        )
    }
    
    /**
     * Get all metrics
     */
    fun getAllMetrics(): ApplicationMetrics {
        val counterMetrics = counters.mapValues { it.value.get() }
        val gaugeMetrics = gauges.mapValues { it.value() }
        val timerMetrics = timers.mapValues { getTimerStats(it.key) }
        
        return ApplicationMetrics(
            counters = counterMetrics,
            gauges = gaugeMetrics,
            timers = timerMetrics.filterValues { it != null } as Map<String, TimerStats>,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Start metrics collection
     */
    suspend fun startMetricsCollection() {
        logger.info("Starting metrics collection...")
        
        // Collect and log metrics every 60 seconds
        while (scope.isActive) {
            try {
                val metrics = getAllMetrics()
                logMetrics(metrics)
                delay(60_000)
            } catch (e: Exception) {
                logger.error("Error during metrics collection", e)
                delay(60_000)
            }
        }
    }
    
    /**
     * Log metrics
     */
    private fun logMetrics(metrics: ApplicationMetrics) {
        logger.info("=== Application Metrics ===")
        logger.info("Counters: ${metrics.counters}")
        logger.info("Gauges: ${metrics.gauges}")
        logger.info("Timers: ${metrics.timers}")
        logger.info("==========================")
    }
    
    /**
     * Shutdown metrics manager
     */
    fun shutdown() {
        logger.info("Shutting down Metrics Manager...")
        scope.coroutineContext.cancelChildren()
        counters.clear()
        gauges.clear()
        timers.clear()
        logger.info("Metrics Manager shutdown complete")
    }
}

/**
 * Timer statistics
 */
data class TimerStats(
    val count: Int,
    val min: Long,
    val max: Long,
    val avg: Double,
    val p50: Long,
    val p95: Long,
    val p99: Long
)

/**
 * Application metrics
 */
data class ApplicationMetrics(
    val counters: Map<String, Long>,
    val gauges: Map<String, Number>,
    val timers: Map<String, TimerStats>,
    val timestamp: Long
)
