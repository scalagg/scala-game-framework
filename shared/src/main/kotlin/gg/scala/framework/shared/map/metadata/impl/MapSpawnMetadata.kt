package gg.scala.framework.shared.map.metadata.impl

import gg.scala.framework.shared.map.metadata.AbstractMapMetadata
import gg.scala.framework.shared.map.metadata.anonymous.Position

/**
 * @author Limo
 * @date 6/17/2025
 */
data class MapSpawnMetadata(
    override val id: String,
    var position: Position
) : AbstractMapMetadata()
{
    override fun report() = "Spawn | $id | $position"
    override fun getAbstractType() = MapSpawnMetadata::class.java
}