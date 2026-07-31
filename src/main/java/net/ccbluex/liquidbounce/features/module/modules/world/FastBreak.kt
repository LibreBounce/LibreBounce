/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

// TODO: Make this able to bypass more anti-cheats, such as NCP
object FastBreak : Module("FastBreak", Category.WORLD) {

    private val breakDamage by float("BreakDamage", 0.8F, 0.1F..1F)

    val onUpdate = handler<UpdateEvent> {
        mc.interactionManager.blockHitDelay = 0

        if (mc.interactionManager.curBlockDamageMP > breakDamage)
            mc.interactionManager.curBlockDamageMP = 1F

        if (Fucker.currentDamage > breakDamage)
            Fucker.currentDamage = 1F

        if (Nuker.currentDamage > breakDamage)
            Nuker.currentDamage = 1F
    }
}
