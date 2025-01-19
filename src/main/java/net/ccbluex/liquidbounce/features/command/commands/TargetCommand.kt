/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.Targets

object TargetCommand : Command("target") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size <= 1) {
            chatSyntax("target <player/mob/animal/invisible>")
        }

        when (args[1].lowercase()) {
            "player" -> {
                Targets.player = !Targets.player
                chat("§7Target player toggled ${if (Targets.player) "on" else "off"}.")
                playEdit()
            }

            "mob" -> {
                Targets.mob = !Targets.mob
                chat("§7Target mob toggled ${if (Targets.mob) "on" else "off"}.")
                playEdit()
            }

            "animal" -> {
                Targets.animal = !Targets.animal
                chat("§7Target animal toggled ${if (Targets.animal) "on" else "off"}.")
                playEdit()
            }

            "invisible" -> {
                Targets.invisible = !Targets.invisible
                chat("§7Target Invisible toggled ${if (Targets.invisible) "on" else "off"}.")
                playEdit()
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> listOf("player", "mob", "animal", "invisible")
                .filter { it.startsWith(args[0], true) }

            else -> emptyList()
        }
    }
}