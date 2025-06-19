package gg.scala.framework.shared

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import gg.scala.framework.shared.map.metadata.AbstractMapMetadata
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.serializers.impl.AbstractTypeSerializer
import org.bukkit.potion.PotionEffectType

/**
 * @author Limo
 * @date 6/17/2025
 */
object FrameworkShared
{
        init
        {
            Serializers.create {
                registerTypeAdapter(
                    AbstractMapMetadata::class.java,
                    AbstractTypeSerializer<AbstractMapMetadata>()
                )

                registerTypeAdapter(
                    PotionEffectType::class.java,
                    object : TypeAdapter<PotionEffectType>()
                    {
                        override fun write(out: JsonWriter?, value: PotionEffectType?)
                        {
                            out?.value(value?.name)
                        }

                        override fun read(`in`: JsonReader?): PotionEffectType?
                        {
                            return PotionEffectType.getByName(`in`?.nextString())
                        }
                    }
                )
            }
        }
    }
