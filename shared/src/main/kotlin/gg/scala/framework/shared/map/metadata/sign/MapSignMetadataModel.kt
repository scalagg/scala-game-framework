package gg.scala.framework.shared.map.metadata.sign

import gg.scala.framework.shared.map.metadata.scanner.AbstractMapMetadataScanner
import org.bukkit.Location

/**
 * @author Limo
 * @date 6/17/2025
 */
data class MapSignMetadataModel(
    val metaType: String,
    val location: Location,
    val id: String,
    val extraMetadata: List<String>,
    val scanner: AbstractMapMetadataScanner<*>
)
{
    fun flags(id: String) = extraMetadata
        .any {
            it == id
        }

    fun valueOf(id: String) = extraMetadata
        .firstOrNull {
            it.startsWith("$id=")
        }
        ?.split("=")?.first()
}