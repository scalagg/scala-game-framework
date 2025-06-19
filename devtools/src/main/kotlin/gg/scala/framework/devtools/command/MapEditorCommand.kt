package gg.scala.framework.devtools.command

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.flavor.inject.Inject
import gg.scala.framework.devtools.FrameworkDevTools
import gg.scala.framework.devtools.editor.mapeditor.MapEditor
import gg.scala.framework.devtools.map.MapManageServices
import net.evilblock.cubed.util.CC
import kotlin.collections.get

/**
 * @author Limo
 * @date 6/17/2025
 */
@AutoRegister
@CommandPermission("op")
@CommandAlias("mapeditor")
object MapEditorCommand : ScalaCommand()
{
    @Inject
    lateinit var plugin: FrameworkDevTools

    @Default
    fun default(player: ScalaPlayer)
    {
        if (MapEditor.instances[player.uniqueId] != null)
        {
            throw ConditionFailedException("Already in editor!")
        }

        player.sendMessage("${CC.GOLD}Entering the editor...")
        MapEditor(player.bukkit(), MapManageServices.loader).apply {
            terminable.bindWith(plugin)
        }.initialize()
    }

    @Subcommand("leave")
    fun leave(player: ScalaPlayer)
    {
        if (MapEditor.instances[player.uniqueId] == null)
        {
            throw ConditionFailedException("Not in editor!")
        }

        MapEditor.instances[player.uniqueId]?.terminable?.closeAndReportException()
        MapEditor.instances.remove(player.uniqueId)

        player.sendMessage("${CC.RED}Left editor.")
    }
}
