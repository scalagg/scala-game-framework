package gg.scala.framework.shared.profile

import gg.scala.framework.shared.statistics.CgsGameStatistics
import gg.scala.store.storage.storable.IDataStoreObject
import net.evilblock.cubed.serializers.impl.AbstractTypeSerializable
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Limo
 * @date 6/17/2025
 */
abstract class CgsProfile(
    override val identifier: UUID
) : AbstractTypeSerializable, IDataStoreObject
{
    abstract fun save(): CompletableFuture<Void>

    var gameKills = CgsGameStatistics()

    var kills = CgsGameStatistics()
    var deaths = CgsGameStatistics()

    var damageDealt = CgsGameStatistics()
    var damageTaken = CgsGameStatistics()

    var wins = CgsGameStatistics()
    var losses = CgsGameStatistics()
}
