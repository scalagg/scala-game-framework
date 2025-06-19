package gg.scala.framework.shared.game

/**
 * @author Limo
 * @date 6/17/2025
 */
data class GameTypeContainer(
    val gameTypes: MutableMap<String, GameType> = mutableMapOf()
)