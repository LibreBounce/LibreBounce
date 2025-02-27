package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import org.lwjgl.glfw.GLFW

private typealias MouseClick = (Slot?, Int, Int, SlotActionType) -> Unit
private typealias ClickAction = (slot: Slot, callback: MouseClick) -> Unit

/**
 * Quick item movement
 *
 * @author sqlerrorthing
 */
object ModuleItemScroller : ClientModule("ItemScroller", Category.MISC) {
    @JvmStatic
    val clickMode by enumChoice("ClickMode", ClickMode.SWAP)

    @JvmStatic
    @Suppress("MagicNumber")
    val delay by int("Delay", 20, 0..500, suffix = "ms")
}

@Suppress("UNUSED")
enum class ClickMode(
    override val choiceName: String,
    val action: ClickAction
) : NamedChoice {
    QUICK_MOVE("QuickMove", { slot, callback ->
        callback(slot, slot.id, GLFW.GLFW_MOUSE_BUTTON_LEFT, SlotActionType.QUICK_MOVE)
    }),

    /**
        TODO: calculate an empty (air) slot
            & swap current slot with an empty (air) slot
     */
    SWAP("Swap", { slot, callback ->
        QUICK_MOVE.action(slot, callback)
    })
}
