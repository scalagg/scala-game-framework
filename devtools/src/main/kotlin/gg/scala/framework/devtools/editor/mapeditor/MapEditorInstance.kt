package gg.scala.framework.devtools.editor.mapeditor

import me.lucko.helper.terminable.Terminable
import org.bukkit.Bukkit
import org.bukkit.World

/**
 * @author Limo
 * @date 6/17/2025
 */
data class MapEditorInstance(
    val slimeWorldName: String,
    val world: World
) : Terminable
{
    override fun close() {
        if (Bukkit.getWorld(world.name) == null) {
            return
        }

        Bukkit.unloadWorld(world, false)
    }
}