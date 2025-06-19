package gg.scala.framework.devtools.editor

import me.lucko.helper.terminable.Terminable
import org.bukkit.Bukkit
import org.bukkit.World
import java.io.File

/**
 * @author Limo
 * @date 6/17/2025
 */
data class EditorInstance(
    val associatedElement: File,
    val world: World
): Terminable
{
    override fun close()
    {
        if (Bukkit.getWorld(world.name) == null)
        {
            return
        }

        Bukkit.unloadWorld(world, false)
    }
}
