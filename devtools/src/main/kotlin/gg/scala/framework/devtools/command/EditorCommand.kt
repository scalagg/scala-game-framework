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
import gg.scala.framework.devtools.editor.Editor
import net.evilblock.cubed.util.CC
import kotlin.collections.get

/**
 * @author Limo
 * @date 6/17/2025
 */
@AutoRegister
@CommandPermission("op")
@CommandAlias("editor")
object EditorCommand : ScalaCommand() {

    @Inject
    lateinit var plugin: FrameworkDevTools

    @Default
    fun default(player: ScalaPlayer) {
        if (Editor.instances[player.uniqueId] != null) {
            throw ConditionFailedException("Already in editor!")
        }

        player.sendMessage("${CC.GOLD}Entering the editor...")
        Editor(player.bukkit()).apply {
            terminable.bindWith(plugin)
        }.initialize()
    }

    @Subcommand("leave")
    fun leave(player: ScalaPlayer) {
        if (Editor.instances[player.uniqueId] == null) {
            throw ConditionFailedException("Not in editor!")
        }

        Editor.instances[player.uniqueId]?.terminable?.closeAndReportException()
        Editor.instances.remove(player.uniqueId)

        player.sendMessage("${CC.RED}Left editor.")
    }
}
