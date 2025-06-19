package gg.scala.framework.devtools.editor

import com.sk89q.worldedit.Vector
import gg.scala.framework.devtools.map.MapManageServices
import gg.scala.framework.devtools.util.WorldEditUtils
import gg.scala.framework.shared.game.GameType
import gg.scala.framework.shared.game.GameTypeService
import gg.scala.framework.shared.map.MapService
import gg.scala.framework.shared.map.utilities.MapMetadataScanUtilities
import gg.scala.framework.shared.map.Map
import gg.scala.framework.shared.map.metadata.impl.MapSpawnMetadata
import gg.scala.lemon.hotbar.HotbarPreset
import gg.scala.lemon.hotbar.HotbarPresetHandler
import gg.scala.lemon.hotbar.entry.impl.DynamicHotbarPresetEntry
import gg.scala.lemon.hotbar.entry.impl.StaticHotbarPresetEntry
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.Menu
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.prompt.InputPrompt
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import java.io.File
import java.util.UUID

/**
 * @author Limo
 * @date 6/17/2025
 */
class Editor(private val player: Player)
{
    companion object
    {
        val instances = mutableMapOf<UUID, Editor>()
    }

    private val containers = Containers()
    private val editorID = "editor-${player.uniqueId}"

    var visiting: EditorInstance? = null
    val schematics = containers.schematicsDirectory.listFiles()
        ?.toList()?.filterNotNull() ?: emptyList()

    val terminable = CompositeTerminable.create()

    fun initialize()
    {
        if (schematics.isEmpty())
        {
            player.sendMessage("${CC.RED}There are no schematics to edit!")
            return
        }

        var sneakTerminable: CompositeTerminable? = null
        Events
            .subscribe(PlayerToggleSneakEvent::class.java)
            .handler {
                if (it.player.uniqueId == player.uniqueId)
                {
                    if (it.isSneaking)
                    {
                        if (sneakTerminable != null)
                        {
                            return@handler
                        }

                        sneakTerminable = CompositeTerminable.create()

                        Schedulers
                            .sync()
                            .runRepeating({ _ ->
                                player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 0.5f)
                            }, 0L, 5L)
                            .bindWith(sneakTerminable!!)

                        Schedulers
                            .sync()
                            .runLater({
                                val menu = object : Menu("Editor ${CC.GOLD}(EXPERIMENTS)")
                                {
                                    init
                                    {
                                        placeholder = true
                                    }

                                    override fun getButtons(player: Player) = mapOf(
                                        4 to ItemBuilder
                                            .of(Material.GLOWSTONE_DUST)
                                            .name("${CC.GOLD}(EXPERIMENT) ${CC.YELLOW}K-Means Island Clustering")
                                            .addToLore("${CC.GREEN}Click to start experiment.")
                                            .toButton { _, _ ->

                                            }
                                    )
                                }

                                menu.openMenu(player)

                                sneakTerminable?.closeAndReportException()
                                sneakTerminable = null
                            }, 20L)
                            .bindWith(sneakTerminable!!)
                    } else
                    {
                        sneakTerminable?.closeAndReportException()
                    }
                }
            }
            .bindWith(terminable)

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                if (it.player.uniqueId == player.uniqueId)
                {
                    sneakTerminable?.closeAndReportException()
                    terminable.closeAndReportException()
                }
            }
            .bindWith(terminable)

        instances[player.uniqueId] = this

        terminable.with {
            val audiences = MapManageServices.audiences.player(player)
            audiences.sendActionBar(Component.empty())

            player.inventory.clear()
            player.updateInventory()

            player.teleport(
                Location(
                    Bukkit.getWorld("world"),
                    0.0, 100.0, 0.0
                )
            )

            HotbarPresetHandler.forget(editorID)

            instances.remove(player.uniqueId)
        }

        Schedulers
            .sync()
            .runRepeating({ task ->
                if (terminable.isClosed)
                {
                    task.closeAndReportException()
                    return@runRepeating
                }

                val audiences = MapManageServices.audiences.player(player)
                val availableSchematics = schematics.size

                audiences.sendActionBar(
                    LegacyComponentSerializer
                        .legacySection()
                        .deserialize(
                            "${CC.B_PRI}Editor ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${
                                "${CC.PRI}Visiting: ${CC.WHITE}${visiting?.associatedElement?.nameWithoutExtension ?: "N/A"}${
                                    if (visiting != null) "${CC.D_GRAY} (${schematics.indexOf(visiting?.associatedElement) + 1}/${schematics.size})" else ""
                                }"
                            } ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${
                                "${CC.PRI}Schematics: ${CC.WHITE}$availableSchematics"
                            }"
                        )
                )
            }, 0L, 10L)
            .bindWith(terminable)

        populateInventoryHotbar()
        visit(schematics.first())
    }

    fun visit(schematic: File)
    {
        val previousInstance = visiting

        val newWorld = EditorGenerator.createNewEmptyWorld()
        WorldEditUtils.paste(
            newWorld, schematic, Vector(0, 64, 0)
        )

        val instance = EditorInstance(schematic, newWorld)
        instance.bindWith(terminable)

        player.gameMode = GameMode.CREATIVE
        player.allowFlight = true
        player.isFlying = true
        player.teleport(
            Location(
                newWorld, 0.0, 96.0, 0.0, -65.0f, 0.0f
            )
        )

        player.sendMessage("${CC.GREEN}Visiting ${CC.WHITE}${schematic.nameWithoutExtension}${CC.GREEN}...")
        player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 1.0f)

        visiting = instance
        previousInstance?.closeAndReportException()
    }

    private fun populateInventoryHotbar()
    {
        val hotbarPreset = HotbarPreset()
        hotbarPreset.addSlot(0, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.MELON)
                    .glow()
                    .name("${CC.RED}Visit Previous ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                val index = schematics.indexOf(visiting!!.associatedElement)
                visit(schematics.getOrNull(index - 1) ?: schematics.last())
            }
        })

        hotbarPreset.addSlot(8, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.SPECKLED_MELON)
                    .glow()
                    .name("${CC.GREEN}Visit Next ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                val index = schematics.indexOf(visiting!!.associatedElement)
                visit(schematics.getOrNull(index + 1) ?: schematics.first())
            }
        })

        hotbarPreset.addSlot(7, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.NAME_TAG)
                    .glow()
                    .name("${CC.YELLOW}Search ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                InputPrompt()
                    .withText("${CC.B_GOLD}Enter the schematic name!")
                    .acceptInput { _, schematic ->
                        val file = schematics.firstOrNull { it.nameWithoutExtension == schematic }
                            ?: return@acceptInput run {
                                player.sendMessage("${CC.RED}No schematic with that name!")
                            }

                        visit(file)
                    }
                    .start(player)
            }
        })

        hotbarPreset.addSlot(6, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.PAINTING)
                    .glow()
                    .name("${CC.GOLD}Schematics ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                GameTypeMenu().openMenu(player)
            }
        })

        hotbarPreset.addSlot(4, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.GOLD_AXE)
                    .glow()
                    .name("${CC.B_YELLOW}Through ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                player.chat("//thru")

                /*InputPrompt()
                    .withText("${CC.B_GOLD}Enter the map ID! ${CC.GRAY}(do not prefix with Map!)")
                    .acceptInput { _, newWorldName ->
                        prepareSlimeWorld(newWorldName)
                    }
                    .start(player)*/
            }
        })

        hotbarPreset.addSlot(
            2, StaticHotbarPresetEntry(
                ItemBuilder.of(Material.SIGN)
                    .name("${CC.WHITE}Metadata Sign")
            )
        )

        hotbarPreset.addSlot(1, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.ENDER_PORTAL_FRAME)
                    .glow()
                    .name("${CC.U_AQUA}Develop Map ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                val bounds = visiting!!.world.loadedChunks.toList().toBounds()
                player.sendMessage("${CC.GREEN}Prepared bounds!")

                val newMapName = visiting!!.associatedElement
                    .nameWithoutExtension
                    .replace("_", "")
                    .capitalize()

                if (MapService.mapWithID(newMapName) != null)
                {
                    player.sendMessage("${CC.RED}A map with this name already exists!")
                    return@context
                }

                val metadata = MapMetadataScanUtilities.buildMetadataFor(bounds, visiting!!.world)

                val availableSpawnMetadata = metadata.metadata.filterIsInstance<MapSpawnMetadata>()
                if (availableSpawnMetadata.size >= 2)
                {
                    player.sendMessage("${CC.B_GRAY}(!)${CC.GRAY} Created a metadata copy! We're now going to build the map data model & store the map in MongoDB...")
                    prepareSlimeWorld(newMapName)

                    val map = Map(
                        name = newMapName.toLowerCase(),
                        bounds = bounds,
                        metadata = metadata,
                        displayName = newMapName.capitalize(),
                        associatedSlimeTemplate = "Map${newMapName.capitalize()}"
                    )

                    with(MapService.cached()) {
                        maps[map.name] = map
                        MapService.sync(this)

                        player.playSound(player.location, Sound.FIREWORK_LAUNCH, 1.0f, 1.0f)
                        player.sendMessage("${CC.B_GREEN}(!)${CC.GREEN} Successfully created map ${CC.YELLOW}${map.name}${CC.GREEN}!")
                    }
                } else
                {
                    player.sendMessage("${CC.B_RED}(!) Failed to create a metadata copy! We found no spawn locations in the map. Please try again.")
                }
            }
        })

        hotbarPreset.addSlot(3, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.ACTIVATOR_RAIL)
                    .glow()
                    .name("${CC.D_RED}Update Map Metadata ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                val bounds = visiting!!.world.loadedChunks.toList().toBounds()
                player.sendMessage("${CC.GREEN}Prepared bounds!")

                val metadata = MapMetadataScanUtilities.buildMetadataFor(bounds, visiting!!.world)

                InputPrompt()
                    .withText("${CC.B_GOLD}Enter the map ID!")
                    .acceptInput { _, newMapName ->
                        val map = MapService.mapWithID(newMapName)
                            ?: return@acceptInput run {
                                player.sendMessage("${CC.RED}No schematic with that name!")
                            }

                        val availableSpawnMetadata = metadata.metadata.filterIsInstance<MapSpawnMetadata>()
                        if (availableSpawnMetadata.size >= 2)
                        {
                            map.metadata = metadata

                            with(MapService.cached()) {
                                maps[map.name] = map
                                MapService.sync(this)

                                player.playSound(player.location, Sound.FIREWORK_LAUNCH, 1.0f, 1.0f)
                                player.sendMessage("${CC.B_GREEN}(!)${CC.GREEN} Updated map ${CC.YELLOW}${map.name}${CC.GREEN}!")
                            }
                        } else
                        {
                            player.sendMessage("${CC.RED}Your metadata is invalid!")
                        }
                    }
                    .start(player)
            }
        })

        hotbarPreset.addSlot(5, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.PAPER)
                    .glow()
                    .name("${CC.AQUA}Report Metadata ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                val bounds = visiting!!.world.loadedChunks.toList().toBounds()
                val metadata = MapMetadataScanUtilities.buildMetadataFor(bounds, visiting!!.world)

                player.sendMessage("${CC.GREEN}[Metadata report ${CC.GRAY}(${metadata.metadata.size})${CC.GREEN}]")
                metadata.metadata.forEach {
                    player.sendMessage("${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${CC.WHITE}${it.report()}")
                }
            }
        })

        hotbarPreset.applyToPlayer(player)
        HotbarPresetHandler.startTrackingHotbar(
            editorID,
            hotbarPreset
        )
    }

    inner class GameTypeMenu : PaginatedMenu() {
        override fun getPrePaginatedTitle(player: Player) = "Select Game Type"

        override fun getAllPagesButtons(player: Player): kotlin.collections.Map<Int, Button> {
            val buttons = mutableMapOf<Int, Button>()

            GameTypeService.cached().gameTypes.values
                .filter { it.enabled }
                .forEach { gameType ->
                    buttons[buttons.size] = ItemBuilder.copyOf(gameType.displayIcon)
                        .name("${CC.GOLD}${gameType.displayName}")
                        .addToLore(*gameType.description.toTypedArray())
                        .addToLore("", "${CC.GRAY}Maps: ${CC.WHITE}${gameType.maps.size}", "", "${CC.YELLOW}Click to view schematics")
                        .toButton { _, _ ->
                            SchematicMenu(gameType).openMenu(player)
                        }
                }

            return buttons
        }
    }

    inner class SchematicMenu(private val gameType: GameType) : PaginatedMenu() {
        override fun getPrePaginatedTitle(player: Player) = "${gameType.displayName} Schematics"

        override fun getAllPagesButtons(player: Player): kotlin.collections.Map<Int, Button> {
            val buttons = mutableMapOf<Int, Button>()
            val gameTypeSchematicsDirectory = File(
                containers.schematicsDirectory,
                gameType.id
            )
            val gameTypeSchematics = gameTypeSchematicsDirectory.listFiles()
                ?.toList()?.filterNotNull() ?: emptyList()

            gameTypeSchematics.forEach {
                buttons[buttons.size] = ItemBuilder
                    .of(Material.MAP)
                    .name("${CC.WHITE}${it.nameWithoutExtension}")
                    .addToLore(
                        "${CC.GREEN}Click to visit!"
                    )
                    .toButton { _, _ ->
                        visit(it)
                    }
            }

            return buttons
        }
    }

    private fun prepareSlimeWorld(newWorldName: String)
    {
        player.sendMessage("${CC.GOLD}Preparing world...")
        player.teleport(
            Location(
                Bukkit.getWorld("world"),
                0.0, 100.0, 0.0
            )
        )

        val worldName = visiting!!.world.name
        Bukkit.unloadWorld(visiting!!.world, true)

        player.sendMessage("${CC.GREEN}Importing into SWM -> MongoDB...")
        MapManageServices.slimePlugin.importWorld(
            File(Bukkit.getWorldContainer(), worldName),
            "Map${newWorldName.capitalize()}",
            MapManageServices.loader
        )

        player.playSound(player.location, Sound.LEVEL_UP, 1.0f, 1.0f)
        player.sendMessage("${CC.GREEN}Imported!")

        val oldSchematic = visiting?.associatedElement
        visiting?.closeAndReportException()
        visiting = null

        if (oldSchematic != null)
        {
            visit(oldSchematic)
        }
    }
}
