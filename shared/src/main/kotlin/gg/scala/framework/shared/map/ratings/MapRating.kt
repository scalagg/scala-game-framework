package gg.scala.framework.shared.map.ratings

import gg.scala.commons.annotations.Model
import gg.scala.store.controller.annotations.Indexed
import gg.scala.store.storage.storable.IDataStoreObject
import java.util.UUID

/**
 * @author Limo
 * @date 6/17/2025
 */
@Model
data class MapRating(
    override val identifier: UUID = UUID.randomUUID(),
    val rating: Int,
    @Indexed val rater: UUID,
    @Indexed val mapID: String
) : IDataStoreObject
