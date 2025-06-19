package gg.scala.framework.shared.map.metadata.scanner

import gg.scala.framework.shared.map.metadata.scanner.impl.MapChestMetadataScanner
import gg.scala.framework.shared.map.metadata.scanner.impl.MapGeneratorMetadataScanner
import gg.scala.framework.shared.map.metadata.scanner.impl.MapLevelMetadataScanner
import gg.scala.framework.shared.map.metadata.scanner.impl.MapPortalMetadataScanner
import gg.scala.framework.shared.map.metadata.scanner.impl.MapShopkeeperMetadataScanner
import gg.scala.framework.shared.map.metadata.scanner.impl.MapSpawnMetadataScanner
import gg.scala.framework.shared.map.metadata.scanner.impl.MapZoneMetadataScanner

/**
 * @author Limo
 * @date 6/17/2025
 */
object MetadataScannerUtilities
{
    private val scanners = mutableListOf<AbstractMapMetadataScanner<*>>(
        MapZoneMetadataScanner,
        MapSpawnMetadataScanner,
        MapLevelMetadataScanner,
        MapPortalMetadataScanner,
        MapChestMetadataScanner,
        MapShopkeeperMetadataScanner,
        MapGeneratorMetadataScanner
    )

    fun matches(type: String) =
        scanners.firstOrNull {
            it.type.equals(type, true)
        }
}
