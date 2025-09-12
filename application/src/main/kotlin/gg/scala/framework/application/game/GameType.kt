package gg.scala.framework.application.game

/**
 * Application-specific GameType that doesn't depend on Minecraft utilities
 * This is separate from the shared GameType to avoid Minecraft dependencies
 */
data class GameType(
    val id: String,
    var displayName: String,
    var dataStore: String = "cgs-${id}",
    var maps: MutableList<String> = mutableListOf(),
    var description: MutableList<String> = mutableListOf(),
    var allowTeams: Boolean = false,
    var maxTeamSize: Int = 1,
    var minTeamSize: Int = 1,
    var enabled: Boolean = false,
    // Application-specific fields
    var resourceLimits: ResourceLimits = ResourceLimits("500m", "1Gi"),
    var portRange: PortRange = PortRange(25565, 25665),
    var scalingConfig: ScalingConfig = ScalingConfig()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameType

        if (id != other.id) return false
        if (displayName != other.displayName) return false
        if (dataStore != other.dataStore) return false
        if (maps != other.maps) return false
        if (description != other.description) return false
        if (allowTeams != other.allowTeams) return false
        if (maxTeamSize != other.maxTeamSize) return false
        if (minTeamSize != other.minTeamSize) return false
        if (enabled != other.enabled) return false
        if (resourceLimits != other.resourceLimits) return false
        if (portRange != other.portRange) return false
        if (scalingConfig != other.scalingConfig) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + dataStore.hashCode()
        result = 31 * result + maps.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + allowTeams.hashCode()
        result = 31 * result + maxTeamSize.hashCode()
        result = 31 * result + minTeamSize.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + resourceLimits.hashCode()
        result = 31 * result + portRange.hashCode()
        result = 31 * result + scalingConfig.hashCode()
        return result
    }
}

/**
 * Resource limits for a game type
 */
data class ResourceLimits(
    val cpu: String,
    val memory: String,
    val storage: String = "2Gi"
)

/**
 * Port range configuration
 */
data class PortRange(
    val startPort: Int,
    val endPort: Int
)

/**
 * Scaling configuration for a game type
 */
data class ScalingConfig(
    val minInstances: Int = 1,
    val maxInstances: Int = 10,
    val scaleUpThreshold: Double = 0.8,
    val scaleDownThreshold: Double = 0.3
)
