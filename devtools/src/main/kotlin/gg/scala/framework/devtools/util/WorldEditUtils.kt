package gg.scala.framework.devtools.util

import com.sk89q.worldedit.CuboidClipboard
import com.sk89q.worldedit.EditSession
import com.sk89q.worldedit.Vector
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.schematic.SchematicFormat
import org.bukkit.World
import java.io.File

/**
 * @author Limo
 * @date 6/17/2025
 */
object WorldEditUtils
{
    fun primeWorldEditApi(world: World?): EditSession? {
        val esFactory = WorldEdit.getInstance().editSessionFactory

        val worldEditWorld = BukkitWorld(world)
        return esFactory.getEditSession(worldEditWorld, Int.Companion.MAX_VALUE)
    }

    @Throws(Exception::class)
    fun paste(world: World?, schematic: File?, pasteAt: Vector?): CuboidClipboard {
        val editSession = primeWorldEditApi(world)
        val clipboard = SchematicFormat.MCEDIT.load(schematic)

        // systems like the ArenaGrid assume that pastes will 'begin' directly at the Vector
        // provided. to ensure we can do this, we manually clear any offset (distance from
        // corner of schematic to player) to ensure our pastes aren't dependant on the
        // location of the player when copied
        clipboard.offset = Vector(0, 0, 0)
        clipboard.paste(editSession, pasteAt, true)

        return clipboard
    }
}