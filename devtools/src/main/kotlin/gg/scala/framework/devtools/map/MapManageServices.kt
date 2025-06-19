package gg.scala.framework.devtools.map

import com.grinderwolf.swm.api.SlimePlugin
import com.grinderwolf.swm.api.loaders.SlimeLoader
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.framework.devtools.FrameworkDevTools
import net.kyori.adventure.platform.bukkit.BukkitAudiences

/**
 * @author Limo
 * @date 6/17/2025
 */
@Service
object MapManageServices {

    @Inject
    lateinit var plugin: FrameworkDevTools

    @Inject
    lateinit var audiences: BukkitAudiences

    lateinit var slimePlugin: SlimePlugin
    lateinit var loader: SlimeLoader

    @Configure
    fun configure() {
        slimePlugin = plugin.server.pluginManager
            .getPlugin("SlimeWorldManager") as SlimePlugin

        loader = slimePlugin.getLoader("mongodb")
    }
}
