package gg.scala.framework.shared.commands.customizers

import gg.scala.commons.acf.ConditionFailedException
import gg.scala.commons.annotations.commands.customizer.CommandManagerCustomizer
import gg.scala.commons.command.ScalaCommandManager
import gg.scala.framework.shared.game.GameType
import gg.scala.framework.shared.game.GameTypeService
import net.evilblock.cubed.util.CC

/**
 * @author Limo
 * @date 6/17/2025
 */
object GameTypeCommandCustomizers
{
    @CommandManagerCustomizer
    fun customize(manager: ScalaCommandManager)
    {
        manager.commandContexts.registerContext(GameType::class.java) {
            val arg = it.popFirstArg()

            GameTypeService.cached().gameTypes[arg]
                ?: throw ConditionFailedException(
                    "No game type with the ID ${CC.YELLOW}$arg${CC.RED} exists."
                )
        }

        manager.commandCompletions
            .registerAsyncCompletion("gameTypes") {
                GameTypeService.cached().gameTypes.keys
            }
    }
}