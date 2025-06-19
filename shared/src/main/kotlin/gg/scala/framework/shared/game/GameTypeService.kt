package gg.scala.framework.shared.game

import gg.scala.commons.persist.datasync.DataSyncKeys
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.commons.persist.datasync.DataSyncSource
import gg.scala.flavor.service.Service
import gg.scala.framework.shared.map.Map
import net.kyori.adventure.key.Key

/**
 * @author Limo
 * @date 6/17/2025
 */
@Service
object GameTypeService : DataSyncService<GameTypeContainer>()
{
        object GameTypeKeys : DataSyncKeys
        {
            override fun newStore() = "cgs-gameTypes"

            override fun store() = Key.key("cgs", "gameTypes")
            override fun sync() = Key.key("cgs", "gsync")
        }

        override fun locatedIn() = DataSyncSource.Mongo

        override fun keys() = GameTypeKeys
        override fun type() = GameTypeContainer::class.java

        fun gameTypesOf(map: Map) = cached().gameTypes.values
            .filter {
                map.name in it.maps
            }
    }