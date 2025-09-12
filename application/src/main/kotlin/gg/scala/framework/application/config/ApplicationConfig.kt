package gg.scala.framework.application.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
 * Application configuration management
 */
object ApplicationConfig {
    
    private val config: Config = ConfigFactory.load()
    
    /**
     * Redis configuration
     */
    object Redis {
        val host: String = config.getString("redis.host")
        val port: Int = config.getInt("redis.port")
        val password: String = config.getString("redis.password")
        val database: Int = config.getInt("redis.database")
        val timeout: Int = config.getInt("redis.timeout")
        
        object Pool {
            val maxTotal: Int = config.getInt("redis.pool.max-total")
            val maxIdle: Int = config.getInt("redis.pool.max-idle")
            val minIdle: Int = config.getInt("redis.pool.min-idle")
        }
    }
    
    /**
     * Deployment strategy configuration
     */
    object Deployment {
        val strategy: String = config.getString("deployment.strategy")
        val autoScaling: Boolean = config.getBoolean("deployment.auto-scaling")
        val maxInstances: Int = config.getInt("deployment.max-instances")
        
        object Scaling {
            object BedWars {
                val minInstances: Int = config.getInt("deployment.scaling.bedwars.min-instances")
                val maxInstances: Int = config.getInt("deployment.scaling.bedwars.max-instances")
                val scaleUpThreshold: Double = config.getDouble("deployment.scaling.bedwars.scale-up-threshold")
                val scaleDownThreshold: Double = config.getDouble("deployment.scaling.bedwars.scale-down-threshold")
            }
            
            object SkyWars {
                val minInstances: Int = config.getInt("deployment.scaling.skywars.min-instances")
                val maxInstances: Int = config.getInt("deployment.scaling.skywars.max-instances")
                val scaleUpThreshold: Double = config.getDouble("deployment.scaling.skywars.scale-up-threshold")
                val scaleDownThreshold: Double = config.getDouble("deployment.scaling.skywars.scale-down-threshold")
            }
            
            object Duels {
                val minInstances: Int = config.getInt("deployment.scaling.duels.min-instances")
                val maxInstances: Int = config.getInt("deployment.scaling.duels.max-instances")
                val scaleUpThreshold: Double = config.getDouble("deployment.scaling.duels.scale-up-threshold")
                val scaleDownThreshold: Double = config.getDouble("deployment.scaling.duels.scale-down-threshold")
            }
        }
    }
    
    /**
     * Kubernetes configuration
     */
    object Kubernetes {
        val namespace: String = config.getString("kubernetes.namespace")
        val configPath: String = config.getString("kubernetes.config-path")
        
        object ResourceQuotas {
            val cpu: String = config.getString("kubernetes.resource-quotas.cpu")
            val memory: String = config.getString("kubernetes.resource-quotas.memory")
            val storage: String = config.getString("kubernetes.resource-quotas.storage")
        }
    }
    
    /**
     * Docker configuration
     */
    object Docker {
        val networkName: String = config.getString("docker.network-name")
        val composeDirectory: String = config.getString("docker.compose-directory")
        val dataDirectory: String = config.getString("docker.data-directory")
        
        object Daemon {
            val socket: String = config.getString("docker.daemon.socket")
            val timeout: Int = config.getInt("docker.daemon.timeout")
        }
    }
    
    /**
     * Game server configuration
     */
    object GameServers {
        val defaultImage: String = config.getString("game-servers.default-image")
        
        object Images {
            val bedwars: String = config.getString("game-servers.images.bedwars")
            val skywars: String = config.getString("game-servers.images.skywars")
            val duels: String = config.getString("game-servers.images.duels")
        }
        
        object DefaultResources {
            val cpu: String = config.getString("game-servers.default-resources.cpu")
            val memory: String = config.getString("game-servers.default-resources.memory")
            val storage: String = config.getString("game-servers.default-resources.storage")
        }
        
        object Resources {
            object BedWars {
                val cpu: String = config.getString("game-servers.resources.bedwars.cpu")
                val memory: String = config.getString("game-servers.resources.bedwars.memory")
                val storage: String = config.getString("game-servers.resources.bedwars.storage")
            }
            
            object SkyWars {
                val cpu: String = config.getString("game-servers.resources.skywars.cpu")
                val memory: String = config.getString("game-servers.resources.skywars.memory")
                val storage: String = config.getString("game-servers.resources.skywars.storage")
            }
            
            object Duels {
                val cpu: String = config.getString("game-servers.resources.duels.cpu")
                val memory: String = config.getString("game-servers.resources.duels.memory")
                val storage: String = config.getString("game-servers.resources.duels.storage")
            }
        }
        
        object Ports {
            val startPort: Int = config.getInt("game-servers.ports.start-port")
            val endPort: Int = config.getInt("game-servers.ports.end-port")
            val increment: Int = config.getInt("game-servers.ports.increment")
        }
    }
    
    /**
     * Queue configuration
     */
    object Queue {
        object Matchmaking {
            val maxWaitTime: Long = config.getLong("queue.matchmaking.max-wait-time")
            val pingThreshold: Int = config.getInt("queue.matchmaking.ping-threshold")
            val eloThreshold: Int = config.getInt("queue.matchmaking.elo-threshold")
        }
        
        object Processing {
            val interval: Long = config.getLong("queue.processing.interval")
            val batchSize: Int = config.getInt("queue.processing.batch-size")
        }
    }
    
    /**
     * Monitoring configuration
     */
    object Monitoring {
        val statusTimeout: Long = config.getLong("monitoring.status-timeout")
        val healthCheckInterval: Long = config.getLong("monitoring.health-check-interval")
        val leaderboardUpdateInterval: Long = config.getLong("monitoring.leaderboard-update-interval")
        
        object Metrics {
            val enabled: Boolean = config.getBoolean("monitoring.metrics.enabled")
            val collectionInterval: Long = config.getLong("monitoring.metrics.collection-interval")
            val retentionDays: Int = config.getInt("monitoring.metrics.retention-days")
        }
    }
    
    /**
     * Logging configuration
     */
    object Logging {
        val level: String = config.getString("logging.level")
        val pattern: String = config.getString("logging.pattern")
        
        object File {
            val enabled: Boolean = config.getBoolean("logging.file.enabled")
            val path: String = config.getString("logging.file.path")
            val maxSize: String = config.getString("logging.file.max-size")
            val maxFiles: Int = config.getInt("logging.file.max-files")
        }
        
        object Console {
            val enabled: Boolean = config.getBoolean("logging.console.enabled")
            val colored: Boolean = config.getBoolean("logging.console.colored")
        }
    }
    
    /**
     * Security configuration
     */
    object Security {
        object Api {
            val enabled: Boolean = config.getBoolean("security.api.enabled")
            val token: String = config.getString("security.api.token")
            val rateLimit: Int = config.getInt("security.api.rate-limit")
        }
        
        object Network {
            val allowedIps: List<String> = config.getStringList("security.network.allowed-ips")
            val blockedIps: List<String> = config.getStringList("security.network.blocked-ips")
        }
    }
    
    /**
     * Performance configuration
     */
    object Performance {
        object Threads {
            val corePoolSize: Int = config.getInt("performance.threads.core-pool-size")
            val maxPoolSize: Int = config.getInt("performance.threads.max-pool-size")
            val queueCapacity: Int = config.getInt("performance.threads.queue-capacity")
        }
        
        object Memory {
            val heapSize: String = config.getString("performance.memory.heap-size")
            val gcType: String = config.getString("performance.memory.gc-type")
        }
    }
}
