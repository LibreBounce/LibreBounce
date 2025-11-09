/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.minecraft.entity.player.EntityPlayer

object PingCommand : Command("ping") {
    override fun execute(args: Array<String>) {
        val ping = (player as EntityPlayer).getPing()

        chat("§3Your ping is §a${ping}ms§3.")
    }
}