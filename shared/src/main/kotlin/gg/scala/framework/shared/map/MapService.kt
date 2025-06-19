package gg.scala.framework.shared.map

import gg.scala.commons.persist.datasync.DataSyncKeys
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.commons.persist.datasync.DataSyncSource
import gg.scala.flavor.service.Service
import net.kyori.adventure.key.Key

/**
 * @author Limo
 * @date 6/17/2025
 */
@Service
object MapService : DataSyncService<MapContainer>()
{
    object MapKeys : DataSyncKeys
    {
        override fun newStore() = "cgs-maps"

        override fun store() = Key.key("cgs", "maps")
        override fun sync() = Key.key("cgs", "msync")
    }

    override fun locatedIn() = DataSyncSource.Mongo

    override fun keys() = MapKeys
    override fun type() = MapContainer::class.java

    fun maps() = cached().maps.values

    fun mapWithID(id: String) = cached().maps.values
        .firstOrNull {
            it.name.equals(id, true)
        }

    fun mapWithSlime(id: String) = cached().maps.values
        .firstOrNull {
            it.associatedSlimeTemplate.equals(id, true)
        }

    var onPostReload = {}
    override fun postReload()
    {
        // Auto-migrate missing description fields
        var changed = false

        cached().maps.values.forEach { map ->
            if (map.description == null) {
                map.description = mutableListOf()
                changed = true
            }
        }

        if (changed) {
            sync(cached()) // Save the updated data back to Mongo
            println("[MapService] Migrated maps with missing description field.")
        }

        onPostReload()
    }
}