package net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance

open class SuperKnockbackMode(val modeName: String) : MinecraftInstance {
    open fun onAttack(event: AttackEvent) {}
    open fun onPacket(event: PacketEvent) {}
    open fun onUpdate(event: UpdateEvent) {}
    open fun onPostSprintUpdate(event: PostSprintUpdateEvent) {}

    open fun onToggle(state: Boolean) {}
}