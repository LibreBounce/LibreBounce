package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.FakeLagEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.blocking.NoSlowBlock.modes
import net.ccbluex.liquidbounce.utils.entity.isBlockAction
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

internal object NoSlowBlockingBlink : Choice("Blink") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    @Suppress("unused")
    private val fakeLagHandler = handler<FakeLagEvent> { event ->
        if (!player.isBlockAction) {
            return@handler
        }

        event.action = if (event.packet is PlayerMoveC2SPacket) {
             FakeLag.Action.QUEUE
        } else if (event.action == FakeLag.Action.FLUSH) {
            FakeLag.Action.PASS
        } else {
            return@handler
        }
    }

}
