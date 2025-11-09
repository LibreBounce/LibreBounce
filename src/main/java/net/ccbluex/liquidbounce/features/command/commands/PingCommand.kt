/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.extensions.getPing

object PingCommand : Command("ping") {
    override fun execute(args: Array<String>) {
        chat("§3Your ping is §a${player.getPing()}ms§3.")
    }
}