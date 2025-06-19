package gg.scala.framework.shared.map.metadata.impl

import gg.scala.framework.shared.map.metadata.AbstractMapMetadata
import gg.scala.framework.shared.map.metadata.anonymous.Position

/**
 * @author Limo
 * @date 6/17/2025
 */
data class MapChestMetadata(
 override val id: String,
 var locations: Map<String, List<Position>>
) : AbstractMapMetadata()
{
 fun getGlobal() = locations["global"] ?: listOf()
// fun getByTeam(teamIdentifier: TeamIdentifier) = locations[teamIdentifier.label.lowercase()] ?: listOf()

 override fun report() = "Chests | $id | ${locations.size} group (${locations.entries.joinToString(", ") { "${it.key} -> ${it.value.size}"}})"
 override fun getAbstractType() = MapChestMetadata::class.java
}
