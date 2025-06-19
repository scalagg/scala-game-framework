package gg.scala.framework.shared.map.metadata.scanner

import gg.scala.framework.shared.map.metadata.AbstractMapMetadata
import gg.scala.framework.shared.map.metadata.sign.MapSignMetadataModel

/**
 * @author Limo
 * @date 6/17/2025
 */
abstract class AbstractMapMetadataScanner<T : AbstractMapMetadata>
{
    abstract val type: String

    abstract fun scan(id: String, models: List<MapSignMetadataModel>): T?
}