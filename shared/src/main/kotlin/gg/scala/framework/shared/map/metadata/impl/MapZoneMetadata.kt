package gg.scala.framework.shared.map.metadata.impl

import gg.scala.framework.shared.map.metadata.AbstractMapMetadata
import gg.scala.framework.shared.map.metadata.anonymous.Bounds
import gg.scala.framework.shared.map.metadata.anonymous.Position

/**
 * @author Limo
 * @date 6/17/2025
 */
data class MapZoneMetadata(
    override val id: String,
    var lower: Position,
    var top: Position,
    val bounds: Bounds = Bounds(lower, top)
) : AbstractMapMetadata()
{
    override fun report() = "Zone | $id | $lower -> $top"
    override fun getAbstractType() = MapZoneMetadata::class.java
}