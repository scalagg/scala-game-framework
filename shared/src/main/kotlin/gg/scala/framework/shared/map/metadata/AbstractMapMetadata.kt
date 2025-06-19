package gg.scala.framework.shared.map.metadata

import net.evilblock.cubed.serializers.impl.AbstractTypeSerializable

/**
 * @author Limo
 * @date 6/17/2025
 */
abstract class AbstractMapMetadata : AbstractTypeSerializable
{
    abstract val id: String
    abstract fun report(): String
}