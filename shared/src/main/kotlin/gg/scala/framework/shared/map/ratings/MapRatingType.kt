package gg.scala.framework.shared.map.ratings

import net.evilblock.cubed.util.CC
import org.bukkit.ChatColor

/**
 * @author Limo
 * @date 6/17/2025
 */
enum class MapRatingType(val color: ChatColor)
{
    One(ChatColor.RED), Two(ChatColor.GOLD), Three(ChatColor.YELLOW), Four(ChatColor.YELLOW), Five(ChatColor.GREEN);

    val format = "$color[${ordinal + 1}✮]${CC.RESET}"
}
