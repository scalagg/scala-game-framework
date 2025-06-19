package gg.scala.framework.shared.commands

import gg.scala.commons.acf.CommandHelp
import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.acf.annotation.CommandAlias
import gg.scala.commons.acf.annotation.CommandCompletion
import gg.scala.commons.acf.annotation.CommandPermission
import gg.scala.commons.acf.annotation.Default
import gg.scala.commons.acf.annotation.Description
import gg.scala.commons.acf.annotation.HelpCommand
import gg.scala.commons.acf.annotation.Subcommand
import gg.scala.commons.annotations.commands.AssignPermission
import gg.scala.commons.annotations.commands.AutoRegister
import gg.scala.commons.command.ScalaCommand
import gg.scala.commons.issuer.ScalaPlayer
import gg.scala.framework.shared.game.GameType
import gg.scala.framework.shared.map.Map
import gg.scala.framework.shared.map.MapService
import net.evilblock.cubed.menu.menus.TextEditorMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.Color
import net.evilblock.cubed.util.bukkit.FancyMessage
import net.md_5.bungee.api.chat.ClickEvent
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * @author Limo
 * @date 6/17/2025
 */
@AutoRegister
@CommandAlias("map")
@CommandPermission("cgs.commands.map")
object MapCommands : ScalaCommand()
{
    @Default
    @HelpCommand
    fun onDefault(help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("info")
    @CommandCompletion("@maps")
    @Description("View information about a map")
    fun onInfo(player: ScalaPlayer, map: Map) {
        player.sendMessage("${CC.GREEN}Information for the map ${CC.B_WHITE}${map.displayName}")
        player.sendMessage(" ")
        player.sendMessage("${CC.GRAY}ID: ${CC.WHITE}${map.name}")
        player.sendMessage(
            "${CC.GRAY}Icon: ${CC.WHITE}${
                map.displayIcon.type.name.lowercase().replaceFirstChar { it.uppercase() }
            }"
        )
        player.sendMessage("${CC.GRAY}Locked: ${if (map.locked) "${CC.GREEN}True" else "${CC.RED}False"}")
        player.sendMessage("${CC.GRAY}Associated Template: ${CC.WHITE}${map.associatedSlimeTemplate}")
        player.sendMessage(" ")
        player.sendMessage("${CC.GRAY}Associated Kit Groups ${CC.WHITE}(${map.associatedGameTypes.size})${CC.GRAY}:")
        if (map.associatedGameTypes.isNotEmpty()) {
            for (group in map.associatedGameTypes) {
                player.sendMessage(" &7▪ &f$group")
            }
        } else {
            player.sendMessage("&cNone Given")
        }
        player.sendMessage(" ")
    }

    @Subcommand("seticon")
    @CommandCompletion("@maps")
    @Description("Enter the icon that you want a map to have.")
    fun onSetIcon(player: ScalaPlayer, map: Map) {
        val itemStack = player.bukkit().itemInHand

        if (itemStack == null || itemStack.type == Material.AIR) {
            throw ConditionFailedException(
                "You must be holding an item to set it as an icon!"
            )
        }

        with(MapService.cached()) {
            map.displayIcon = itemStack
            MapService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have updated ${map.displayName}'s icon to ${CC.YELLOW}${
                itemStack.type.name.lowercase().replaceFirstChar { it.uppercase() }
            }${CC.GREEN}."
        )
    }

    @Subcommand("setname")
    @CommandCompletion("@maps *")
    @Description("Update a map's name/ID.")
    fun onNameChange(player: ScalaPlayer, map: Map, name: String) {
        val oldDisplayName = map.name

        if (MapService.mapWithID(name) != null) {
            throw ConditionFailedException(
                "A map with that ID already exists. Please choose another one."
            )
        }

        with(MapService.cached()) {
            map.name = name
            MapService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have changed $oldDisplayName's name to ${CC.YELLOW}$name${CC.GREEN}."
        )
    }

    @AssignPermission
    @Subcommand("description")
    @CommandCompletion("@maps *")
    @Description("Edit a map's description.")
    fun onDescription(player: ScalaPlayer, map: Map) {
        object : TextEditorMenu(map.description) {
            override fun getPrePaginatedTitle(player: Player) =
                "Editing: ${map.name} Description..."

            override fun onClose(player: Player) {

            }

            override fun onSave(player: Player, list: List<String>) {
                map.description.clear()
                map.description.addAll(list)

                with(MapService.cached())
                {
                    this.maps[map.name] = map
                    MapService.sync(this)
                }
                player.sendMessage("${CC.GREEN}Saved ${map.name} description!")
            }
        }.openMenu(player.bukkit())
    }

    @Subcommand("setdisplayname")
    @CommandCompletion("@maps *")
    @Description("Update a map's display name.")
    fun onDisplayNameChange(player: ScalaPlayer, map: Map, name: String) {
        val oldDisplayName = map.displayName

        with(MapService.cached()) {
            map.displayName = Color.translate(name)
            MapService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}You have changed $oldDisplayName's display name to ${CC.YELLOW}$name${CC.GREEN}."
        )
    }

//    @Subcommand("delete")
//    @CommandCompletion("@maps")
//    @Description("Deletes a map based on the input given.")
//    fun onDelete(player: ScalaPlayer, mapName: String): CompletableFuture<Void> {
//        val map = MapService.mapWithID(mapName)
//            ?: throw ConditionFailedException(
//                "A map with the ID ${CC.YELLOW}$mapName ${CC.RED}does not exist."
//            )
//
//        return GameManagerService.allGames()
//            .thenApply {
//                it.filter { ref ->
//                    ref.mapID == map.name
//                }
//            }
//            .thenAccept {
//                if (it.isNotEmpty())
//                {
//                    throw ConditionFailedException(
//                        "You cannot delete this map as there are games ongoing bound to this map ID. Lock the map from being used in any new games by using ${CC.BOLD}/map lock ${map.name}${CC.RED}."
//                    )
//                }
//
//        with(MapService.cached()) {
//            maps.remove(map.name)
//            MapService.sync(this)
//        }
//
//        player.sendMessage(
//            "${CC.GREEN}You deleted the map with the ID ${CC.YELLOW}${map.name}${CC.GREEN}."
//        )
//    }

//    @AssignPermission
//    @Subcommand("ratings")
//    @Description("View the average ratings of every map.")
//    fun onRatingOverview(player: ScalaPlayer)
//    {
//        MapRatingOverviewMenu().openMenu(player.bukkit())
//    }

    @AssignPermission
    @Subcommand("lock|unlock")
    @CommandCompletion("@maps")
    @Description("Lock a map from being used in any new games.")
    fun onLock(player: ScalaPlayer, map: Map)
    {
        with(MapService.cached()) {
            map.locked = !map.locked
            MapService.sync(this)
        }

        player.sendMessage(
            "${CC.GREEN}The map lock is now: ${CC.YELLOW}${map.locked}"
        )
    }

    @Subcommand("list")
    @Description("List all maps.")
    fun onList(player: ScalaPlayer)
    {
        val maps = MapService.cached().maps.values
        player.sendMessage(
            "${CC.GREEN}All maps:",
            "${CC.WHITE}[click a map to view information]"
        )

        val listFancyMessage = FancyMessage()
        for ((i, group) in maps.withIndex())
        {
            val uniqueInfoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}${group.name}${CC.WHITE}${
                        if (i != (maps.size - 1)) ", " else "."
                    }"
                )
                .andHoverOf(
                    "${CC.GRAY}Click to view map information."
                )
                .andCommandOf(
                    ClickEvent.Action.RUN_COMMAND,
                    "/map info ${group.name}"
                )

            listFancyMessage.components.addAll(
                uniqueInfoComponent.components
            )
        }

        if (listFancyMessage.components.isNotEmpty())
        {
            listFancyMessage.sendToPlayer(player.bukkit())
        } else
        {
            player.sendMessage(
                "${CC.RED}None"
            )
        }
    }

    @Subcommand("groups add")
    @CommandCompletion("@maps @stranger-kit-groups")
    @Description("Associate a kit group with a map.")
    fun onGroupsAdd(player: ScalaPlayer, map: Map, group: GameType)
    {
        if (group.id in map.associatedGameTypes)
        {
            throw ConditionFailedException(
                "The kit group ${CC.YELLOW}${group.id}${CC.RED} is already associated with map ${CC.YELLOW}${map.name}${CC.RED}."
            )
        }

        map.associatedGameTypes += group.id
        MapService.sync(MapService.cached())

        player.sendMessage(
            "${CC.GREEN}Kit group ${CC.YELLOW}${group.id}${CC.GREEN} is now associated with map ${CC.YELLOW}${map.name}${CC.GREEN}."
        )
    }

    @Subcommand("groups remove")
    @CommandCompletion("@maps @associated-kit-groups")
    @Description("Remove an associated kit group from a map.")
    fun onGroupsRemove(player: ScalaPlayer, map: Map, group: GameType)
    {
        if (group.id !in map.associatedGameTypes)
        {
            throw ConditionFailedException(
                "The kit group ${CC.YELLOW}${group.id}${CC.RED} is not associated with map ${CC.YELLOW}${map.name}${CC.RED}."
            )
        }

        if (map.associatedGameTypes.size == 1)
        {
            throw ConditionFailedException(
                "You cannot remove this kit group when there are no other kit groups associated with this map. Please run ${CC.YELLOW}/map groups add ${map.name} __default__${CC.RED} to add back the default group before you remove the ${CC.YELLOW}${group.id}${CC.RED} group."
            )
        }

        map.associatedGameTypes -= group.id
        MapService.sync(MapService.cached())

        player.sendMessage(
            "${CC.RED}Kit group ${CC.YELLOW}${group.id}${CC.RED} is no longer associated with map ${CC.YELLOW}${map.name}${CC.RED}."
        )
    }

    @Subcommand("groups list")
    @CommandCompletion("@maps")
    @Description("List all associated kit groups for a map.")
    fun onGroupsList(player: ScalaPlayer, map: Map)
    {
        player.sendMessage(
            "${CC.GREEN}All associated kit groups for map ${CC.B_WHITE}${map.name}${CC.GREEN}:",
            "${CC.WHITE}[click a kit group to view information]"
        )

        val listFancyMessage = FancyMessage()
        for ((i, group) in map.associatedGameTypes.withIndex())
        {
            val uniqueInfoComponent = FancyMessage()
                .withMessage(
                    "${CC.GRAY}$group${CC.WHITE}${
                        if (i != (map.associatedGameTypes.size - 1)) ", " else "."
                    }"
                )
                .andHoverOf(
                    "${CC.GRAY}Click to view map information."
                )
                .andCommandOf(
                    ClickEvent.Action.RUN_COMMAND,
                    "/kitgroup info $group"
                )

            listFancyMessage.components.addAll(
                uniqueInfoComponent.components
            )
        }

        if (listFancyMessage.components.isNotEmpty())
        {
            listFancyMessage.sendToPlayer(player.bukkit())
        } else
        {
            player.sendMessage(
                "${CC.RED}None"
            )
        }
    }
}