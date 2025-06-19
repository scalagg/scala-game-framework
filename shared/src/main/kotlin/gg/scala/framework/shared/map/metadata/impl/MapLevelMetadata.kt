package gg.scala.framework.shared.map.metadata.impl

import gg.scala.framework.shared.map.metadata.AbstractMapMetadata

/**
 * @author Limo
 * @date 6/17/2025
 */
data class MapLevelMetadata(
    val yAxis: Int,
    val below: Int = 2,
    val allowBuildOnBlockSideBlockFaces: Boolean = false,
    override val id: String = "level",
) : AbstractMapMetadata()
{
    val range: IntRange
        get() = (yAxis - below)..yAxis

    override fun report() = "Level | $id | ${if (allowBuildOnBlockSideBlockFaces) "Allows build on side" else "N/A"} | $below blocks below | At $yAxis"
    override fun getAbstractType() = MapLevelMetadata::class.java
}
