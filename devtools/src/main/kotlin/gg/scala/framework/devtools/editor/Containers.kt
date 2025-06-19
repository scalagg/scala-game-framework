package gg.scala.framework.devtools.editor

import org.bukkit.Bukkit
import java.io.File

/**
 * @author Limo
 * @date 6/17/2025
 */
class Containers
{
    val pluginsDirectory = File(
        Bukkit.getWorldContainer(),
        "plugins"
    )

    val worldEditDirectory = File(
        pluginsDirectory,
        "WorldEdit"
    )

    val schematicsDirectory = File(
        worldEditDirectory,
        "schematics"
    )
}
