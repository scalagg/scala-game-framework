package gg.scala.framework.shared.map.metadata.scanner.impl

import gg.scala.framework.shared.map.metadata.anonymous.toPosition
import gg.scala.framework.shared.map.metadata.impl.MapPortalMetadata
import gg.scala.framework.shared.map.metadata.scanner.AbstractMapMetadataScanner
import gg.scala.framework.shared.map.metadata.sign.MapSignMetadataModel

/**
 * @author Limo
 * @date 6/17/2025
 */
object MapPortalMetadataScanner : AbstractMapMetadataScanner<MapPortalMetadata>()
{
    override val type = "portal"

    override fun scan(
        id: String,
        models: List<MapSignMetadataModel>
    ): MapPortalMetadata?
    {
        if (models.size != 2)
        {
            return null
        }

        return MapPortalMetadata(
            id,
            models[0].location.toPosition(),
            models[1].location.toPosition()
        )
    }
}
