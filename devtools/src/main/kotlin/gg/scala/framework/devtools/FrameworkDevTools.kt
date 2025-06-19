package gg.scala.framework.devtools

import gg.scala.commons.ExtendedScalaPlugin
import gg.scala.commons.annotations.container.ContainerDisable
import gg.scala.commons.annotations.container.ContainerEnable
import gg.scala.commons.annotations.container.ContainerPreEnable
import gg.scala.commons.core.plugin.Plugin
import gg.scala.commons.core.plugin.PluginAuthor
import gg.scala.commons.core.plugin.PluginDependency
import gg.scala.commons.core.plugin.PluginDependencyComposite
import gg.scala.commons.core.plugin.PluginWebsite
import gg.scala.framework.shared.FrameworkShared
import me.lucko.helper.Events
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.EntitySpawnEvent
import kotlin.io.deleteRecursively
import kotlin.properties.Delegates

/**
 * @author Limo
 * @date 6/17/2025
 */
@Plugin(
    name = "ScFramework-DevTools",
    version = "%remote%/%branch%/%id%"
)
@PluginAuthor("Scala")
@PluginWebsite("https://scala.gg")
@PluginDependencyComposite(
    PluginDependency("scala-commons"),
    PluginDependency("Lemon"),
    PluginDependency("WorldEdit"),
    PluginDependency("ScBasics", soft = true)
)
class FrameworkDevTools : ExtendedScalaPlugin()
{
    companion object
    {
        @JvmStatic
        var instance by Delegates.notNull<FrameworkDevTools>()
    }

    init
    {
        FrameworkShared
    }

    @ContainerPreEnable
    fun preEnable()
    {
        instance = this
    }

    @ContainerEnable
    fun containerEnable()
    {
        Bukkit.getWorldContainer().listFiles()
            ?.filter { it.name.startsWith("editor") }
            ?.forEach {
                it.deleteRecursively()
            }

        Events
            .subscribe(EntitySpawnEvent::class.java)
            .handler {
                if (it.entityType != EntityType.PLAYER)
                {
                    return@handler
                }

                it.isCancelled = true
            }
            .bindWith(this)
    }

    @ContainerDisable
    fun containerDisable()
    {
        Bukkit.getWorlds()
            .filter { it.name.startsWith("editor") }
            .forEach {
                Bukkit.unloadWorld(it, false)
            }
    }
}