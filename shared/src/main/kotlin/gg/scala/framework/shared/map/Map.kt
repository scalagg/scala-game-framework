package gg.scala.framework.shared.map

import gg.scala.framework.shared.map.metadata.anonymous.Bounds
import gg.scala.framework.shared.map.metadata.anonymous.toPosition
import gg.scala.framework.shared.map.metadata.impl.MapLevelMetadata
import gg.scala.framework.shared.map.metadata.impl.MapSpawnMetadata
import gg.scala.framework.shared.map.metadata.impl.MapZoneMetadata
import gg.scala.framework.shared.map.utilities.MapMetadata
import net.evilblock.cubed.util.bukkit.ItemBuilder
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack

/**
 * @author Limo
 * @date 6/17/2025
 */
data class Map(
    var name: String,
    val bounds: Bounds,
    var metadata: MapMetadata,
    var displayName: String,
    var description: MutableList<String> = mutableListOf(),
    var displayIcon: ItemStack = ItemBuilder
        .of(Material.MAP)
        .build(),
    val associatedSlimeTemplate: String,
    val associatedGameTypes: MutableSet<String> =
        mutableSetOf("__default__")
) {
    var locked = false

    fun findSpawnLocations() = metadata
        .metadata
        .filterIsInstance<MapSpawnMetadata>()

    fun findMapLevelRestrictions() = metadata
        .metadata
        .filterIsInstance<MapLevelMetadata>()
        .firstOrNull()

    fun findZoneContainingEntity(entity: Entity) = metadata
        .metadata
        .filterIsInstance<MapZoneMetadata>()
        .firstOrNull {
            it.bounds.contains(entity.location.toPosition())
        }

    fun findZoneContainingBlock(block: Block) = metadata
        .metadata
        .filterIsInstance<MapZoneMetadata>()
        .firstOrNull {
            it.bounds.contains(block.location.toPosition())
        }

//    fun findSpawnLocationMatchingTeam(team: TeamIdentifier) = metadata
//        .metadata
//        .filterIsInstance<MapSpawnMetadata>()
//        .firstOrNull {
//            it.id == team.label.lowercase()
//        }
//        ?.position
}