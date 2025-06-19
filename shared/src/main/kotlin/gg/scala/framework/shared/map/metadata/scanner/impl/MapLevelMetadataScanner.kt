package gg.scala.framework.shared.map.metadata.scanner.impl

import gg.scala.framework.shared.map.metadata.impl.MapLevelMetadata
import gg.scala.framework.shared.map.metadata.scanner.AbstractMapMetadataScanner
import gg.scala.framework.shared.map.metadata.sign.MapSignMetadataModel

/**
 * @author Limo
 * @date 6/17/2025
 */
object MapLevelMetadataScanner : AbstractMapMetadataScanner<MapLevelMetadata>()
{
    override val type = "level"

    override fun scan(
        id: String,
        models: List<MapSignMetadataModel>
    ): MapLevelMetadata
    {
        val model = models.first()

        return MapLevelMetadata(
            model.location.blockY,
            model.valueOf("below")?.toInt() ?: 2,
            model.flags("allowBuildOnEx")
        )
    }
}
