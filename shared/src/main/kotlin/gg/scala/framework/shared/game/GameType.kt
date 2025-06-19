package gg.scala.framework.shared.game

import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * @author Limo
 * @date 6/17/2025
 */
data class GameType(
    val id: String,
    var displayName: String,
    var dataStore: String = "cgs-${id}",
    var maps: MutableList<String> = mutableListOf(),
    var description: MutableList<String> = mutableListOf(),
    var displayIcon: ItemStack = ItemBuilder
        .of(Material.IRON_SWORD)
        .build(),
    var allowTeams: Boolean = false,
    var maxTeamSize: Int = 1,
    var minTeamSize: Int = 1,
    var enabled: Boolean = false,
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
        if (displayIcon != other.displayIcon) return false
        if (maxTeamSize != other.maxTeamSize) return false
        if (minTeamSize != other.minTeamSize) return false
        if (enabled != other.enabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + dataStore.hashCode()
        result = 31 * result + maps.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + displayIcon.hashCode()
        result = 31 * result + maxTeamSize.hashCode()
        result = 31 * result + minTeamSize.hashCode()
        result = 31 * result + enabled.hashCode()
        return result
    }
}
