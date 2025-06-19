package gg.scala.framework.shared.map.metadata.scanner.impl

import gg.scala.framework.shared.map.metadata.anonymous.toPosition
import gg.scala.framework.shared.map.metadata.impl.MapChestMetadata
import gg.scala.framework.shared.map.metadata.impl.MapGeneratorMetadata
import gg.scala.framework.shared.map.metadata.scanner.AbstractMapMetadataScanner
import gg.scala.framework.shared.map.metadata.sign.MapSignMetadataModel
import org.bukkit.block.BlockFace
import org.bukkit.material.Sign

/**
 * @author Limo
 * @date 6/19/2025
 */
object MapGeneratorMetadataScanner : AbstractMapMetadataScanner<MapGeneratorMetadata>()
{
    override val type = "generator"

    val manualMappings = mutableMapOf(
        BlockFace.NORTH to 180.0F,
        BlockFace.WEST to 90.0F,
        BlockFace.SOUTH to 0.0F,
        BlockFace.EAST to -90.0F,

        BlockFace.SOUTH_WEST to 45.0F,
        BlockFace.SOUTH_EAST to -45.0F,
        BlockFace.NORTH_WEST to 135.0F,
        BlockFace.NORTH_EAST to -135.0F
    )

    val inverseValuesMappings = manualMappings.map { it.value to it.key }
        .associate { it.first to it.second }

    override fun scan(
        id: String, models: List<MapSignMetadataModel>
    ): MapGeneratorMetadata
    {
        val model = models
            .groupBy {
                val scopedID = it.extraMetadata.firstOrNull()
                if (scopedID.isNullOrBlank())
                {
                    return@groupBy "global"
                }

                return@groupBy scopedID
            }
            .mapValues { grouped ->
                grouped.value.map { model ->
                    val location = model.location.clone()

                    val sign = model.location.block.state.data as Sign
                    location.yaw = manualMappings[sign.facing]!!

                    location.z += 0.500F
                    location.x += 0.500F
                    location.toPosition()
                }
            }

        return MapGeneratorMetadata(id, model)
    }
}