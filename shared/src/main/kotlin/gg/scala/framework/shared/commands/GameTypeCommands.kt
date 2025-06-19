package gg.scala.framework.shared.commands

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.*
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.framework.shared.game.GameType
import gg.scala.framework.shared.game.GameTypeService
import net.evilblock.cubed.util.CC
import org.bukkit.Material
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Limo
 * @date 6/17/2025
 */
@AutoRegister
@CommandPermission("cgs.command.gametype")
@CommandAlias("gametype")
object GameTypeCommands : ScalaCommand()
{

    @Default
    @HelpCommand
    fun onDefault(help: CommandHelp)
    {
        help.showHelp()
    }

    @AssignPermission
    @Subcommand("create")
    @Description("Create a new kit.")
    fun onCreate(player: ScalaPlayer, @Single id: String)
    {
        val lowercaseID = id.lowercase()

        if (GameTypeService.cached().gameTypes[lowercaseID] != null)
        {
            throw ConditionFailedException(
                "A game type with the ID ${CC.YELLOW}$lowercaseID${CC.RED} already exists."
            )
        }

        val gameType = GameType(
            id = lowercaseID,
            displayName = id
                .replaceFirstChar {
                    if (it.isLowerCase())
                        it.titlecase(Locale.getDefault())
                    else
                        it.toString()
                }
        )

        with(GameTypeService.cached()) {
            gameTypes[lowercaseID] = gameType
            }

        player.sendMessage(
            "${CC.GREEN}You created a new kit with the ID ${CC.YELLOW}$lowercaseID${CC.GREEN}."
        )
    }

    @AssignPermission
    @Subcommand("delete")
    @CommandCompletion("@gameTypes")
    @Description("Delete an existing game type.")
    fun onDelete(player: ScalaPlayer, gameType: GameType): CompletableFuture<Void>
    {
        return CompletableFuture.runAsync {
            if (GameTypeService.cached().gameTypes.remove(gameType.id) == null) {
                throw ConditionFailedException("No game type with the ID ${CC.YELLOW}${gameType.id}${CC.RED} exists.")
            }

            GameTypeService.sync(GameTypeService.cached())

            player.sendMessage("${CC.GREEN}You have deleted the game type ${CC.YELLOW}${gameType.displayName}${CC.GREEN}.")
        }
    }

    @AssignPermission
    @Subcommand("info")
    @CommandCompletion("@gameTypes")
    @Description("Show information about a given game type.")
    fun onInfo(player: ScalaPlayer, gameType: GameType)
    {
        player.sendMessage("${CC.GREEN}Information for the Game Type ${CC.B_WHITE}${gameType.displayName}")
        player.sendMessage(" ")
        player.sendMessage("${CC.GRAY}ID: ${CC.WHITE}${gameType.id}")
        player.sendMessage("${CC.GRAY}Description: ${CC.WHITE}${gameType.description}")
        player.sendMessage("${CC.GRAY}Enabled: ${if (gameType.enabled) "${CC.GREEN}True" else "${CC.RED}False"}")
        player.sendMessage("${CC.GRAY}Icon: ${CC.WHITE}${
            gameType.displayIcon.type.name.replaceFirstChar {
                if (it.isLowerCase())
                    it.titlecase(Locale.getDefault())
                else
                    it.toString()
            }
        }")
        player.sendMessage(" ")
        player.sendMessage(
        "${CC.PRI}Map bindings:",
        *GameTypeService.cached().gameTypes.values
        .filter { gameType.maps.contains(it.id) }
        .map { " - " + it.displayName }
        .toTypedArray()
        )
    }

    @AssignPermission
    @Subcommand("toggle")
    @CommandCompletion("@gameTypes")
    @Description("Toggle a game type's enabled state.")
    fun onToggle(player: ScalaPlayer, gameType: GameType)
    {
        gameType.enabled = !gameType.enabled

        with(GameTypeService.cached()) {
            GameTypeService.cached().gameTypes[gameType.id] = gameType
            GameTypeService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have ${if (gameType.enabled) "${CC.B_GREEN}enabled" else "${CC.RED}disabled"} ${CC.GREEN}the game type ${CC.YELLOW}${gameType.displayName}${CC.GREEN}."
        )
    }

    @AssignPermission
    @Subcommand("displayname")
    @CommandCompletion("@gameTypes")
    @Description("Edit a game type's display name.")
    fun onDisplayName(player: ScalaPlayer, gameType: GameType, displayName: String)
    {
        gameType.displayName = displayName

        with(GameTypeService.cached()) {
            GameTypeService.cached().gameTypes[gameType.id] = gameType
            GameTypeService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have set the Display Name for the game type ${CC.YELLOW}${gameType.displayName}${CC.GREEN} to:",
            displayName
        )
    }

    @AssignPermission
    @Subcommand("data-store")
    @CommandCompletion("@gameTypes")
    @Description("Edit a game type's data storage.")
    fun onDataStore(player: ScalaPlayer, gameType: GameType, dataStore: String)
    {
        gameType.dataStore = dataStore

        with(GameTypeService.cached()) {
            GameTypeService.cached().gameTypes[gameType.id] = gameType
            GameTypeService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have set the Data Store for the game type ${CC.YELLOW}${gameType.displayName}${CC.GREEN} to:",
            dataStore
        )
    }

    @AssignPermission
    @Subcommand("seticon")
    @CommandCompletion("@gameTypes")
    @Description("Set a gameType's display icon to the item you have held in your hand.")
    fun onDisplayIcon(player: ScalaPlayer, gameType: GameType)
    {
        val item = player.bukkit().inventory.itemInHand
        if (item == null || item.type == Material.AIR)
        {
            throw ConditionFailedException(
                "You must be holding an item in your hand to set it as the display icon for the game type ${CC.YELLOW}${gameType.displayName}${CC.RED}."
            )
        }

        gameType.displayIcon = item

        with(GameTypeService.cached()) {
            GameTypeService.cached().gameTypes[gameType.id] = gameType
            GameTypeService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have set the display icon for the game type ${CC.YELLOW}${gameType.displayName}${CC.GREEN} to the item in your hand with the type ${CC.WHITE}${
                item.type.name.lowercase().replace("_", " ")
            }${CC.GREEN}."
        )
    }
}