package gg.scala.framework.shared.map.utilities

import gg.scala.framework.shared.map.metadata.AbstractMapMetadata
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.util.Vector

/**
 * @author Limo
 * @date 6/17/2025
 */
data class MapMetadata(
    val metadataSignLocations: List<Vector>,
    val metadata: List<AbstractMapMetadata>
)
{
    fun clearSignLocations(world: World)
    {
        val blocks = metadataSignLocations
            .mapNotNull {
                world.getBlockAt(it.blockX, it.blockY, it.blockZ)
            }

        blocks.forEach {
            if (it.type == Material.SIGN_POST || it.type == Material.WALL_SIGN)
            {
                it.type = Material.AIR
            }
        }
    }
}
