package gg.scala.framework.shared.map.metadata.anonymous

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.Vector

/**
 * @author Limo
 * @date 6/17/2025
 */
data class Position(
    val x: Double, val y: Double, val z: Double,
    val yaw: Float = 0.0f, val pitch: Float = 0.0f
)
{
    override fun toString() = "($x, $y, $z)"

    fun isEqualToVector(other: Position) = other.x == x && other.y == y && other.z == z

    fun toVector() = Vector(x, y, z)
    fun toLocation(world: World) = Location(world, x, y, z, yaw, pitch)
}
