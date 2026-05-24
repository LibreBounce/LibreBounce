/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.noweb

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.aac.AAC
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.aac.LAAC
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.intave.IntaveNew
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.intave.IntaveOld
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.other.None
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.other.OldGrim
import net.ccbluex.liquidbounce.features.module.modules.movement.noweb.modes.other.Rewi

object NoWeb : Module("NoWeb", Category.MOVEMENT) {

    private val noWebModes = arrayOf(
        // Vanilla
        None,

        // AAC
        AAC, LAAC,

        // Intave
        IntaveOld,
        IntaveNew,

        // Other
        Rewi,
        OldGrim
    )

    private val modes = noWebModes.map { it.modeName }.toTypedArray()

    val mode by choices(
        "Mode", modes, "None"
    )

    val onUpdate = handler<UpdateEvent> {
        modeModule.onUpdate()
    }

    override val tag
        get() = mode

    private val modeModule
        get() = noWebModes.find { it.modeName == mode }!!
}
