/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object AutoDisable : Module("AutoDisable", Category.MISC, gameDetecting = false) {
    private val modulesList = hashSetOf(KillAura, Scaffold, Fly, Speed)

    private val onFlagged by boolean("onFlag", true)
    private val onWorldChange by boolean("onWorldChange", false)
    private val onDeath by boolean("onDeath", false)

    private val warn by choices("Warn", arrayOf("Chat", "Notification"), "Chat")

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is S08PacketPlayerPosLook && onFlagged) {
            disabled("flagged")
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        if (onDeath && player.isDead) {
            disabled("deaths")
        }
    }

    val onWorld = handler<WorldEvent> {
        if (onWorldChange) {
            disabled("world changed")
        }
    }

    private fun disabled(reason: String) {
        val enabledModules = modulesList.filter { it.state }

        if (enabledModules.isNotEmpty()) {
            enabledModules.forEach { module ->
                module.state = false
            }

            if (warn == "Chat") {
                chat("§eModules have been disabled due to §c$reason")
            } else {
                hud.addNotification(Notification.informative(this, "Modules have been disabled due to $reason", 2000F))
            }
        }
    }

    fun addModule(module: Module) {
        if (!modulesList.contains(module)) {
            modulesList.add(module)
        }
    }

    fun removeModule(module: Module) {
        modulesList.remove(module)
    }

    fun getModules(): Collection<Module> {
        return modulesList
    }
}
