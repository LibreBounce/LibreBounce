/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage
import net.minecraft.client.render.Window
import net.minecraft.resource.Identifier

object TacoCommand : Command("taco"), Listenable {
    var tacoToggle = false
    private var image = 0
    private var running = 0f
    private val tacoTextures = arrayOf(
        Identifier("librebounce/taco/1.png"),
        Identifier("librebounce/taco/2.png"),
        Identifier("librebounce/taco/3.png"),
        Identifier("librebounce/taco/4.png"),
        Identifier("librebounce/taco/5.png"),
        Identifier("librebounce/taco/6.png"),
        Identifier("librebounce/taco/7.png"),
        Identifier("librebounce/taco/8.png"),
        Identifier("librebounce/taco/9.png"),
        Identifier("librebounce/taco/10.png"),
        Identifier("librebounce/taco/11.png"),
        Identifier("librebounce/taco/12.png")
    )

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        tacoToggle = !tacoToggle
        chat(if (tacoToggle) "§aTACO TACO TACO. :)" else "§cYou made the little taco sad! :(")
    }

    val onRender2D = handler<Render2DEvent> {
        if (!tacoToggle)
            return@handler

        running += 0.15f * deltaTime
        val (width, height) = Window(mc)
        drawImage(tacoTextures[image], running.toInt(), height - 60, 64, 32)
        if (width <= running)
            running = -64f
    }

    val onUpdate = handler<UpdateEvent> {
        if (!tacoToggle) {
            image = 0
            return@handler
        }

        image++
        if (image >= tacoTextures.size) image = 0
    }


    override fun tabComplete(args: Array<String>) = listOf("TACO")
}