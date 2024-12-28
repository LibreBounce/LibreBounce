package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.minecraft.client.network.PlayerListEntry
import java.awt.Color


/**
 * ModuleBetterTab
 *
 * @author sqlerrorthing
 * @since 12/28/2024
 **/
object ModuleBetterTab : ClientModule("BetterTab", Category.MISC) {

    init {
        treeAll(
            Limits,
            Highlight,
            AccurateLatency
        )
    }

    val sorting by enumChoice("Sorting", Sorting.VANILLA)

    object AccurateLatency : ToggleableConfigurable(ModuleBetterTab, "AccurateLatency", true) {
        val suffix by boolean("AppendMSSuffix", true)
    }

    object Limits : Configurable("Limits") {
        val tabSize by int("TabSize", 80, 1..1000)
        val height by int("ColumnHeight", 20, 1..100)
    }

    object Highlight : ToggleableConfigurable(ModuleBetterTab, "Highlight", true) {
        class HighlightColored(name: String, color: Color4b) : ToggleableConfigurable(this, name, true) {
            val color by color("Color", color)
        }

        val self = tree(HighlightColored("Self", Color4b(Color(50, 193, 50, 80))))
        val friends = tree(HighlightColored("Friends", Color4b(Color(16, 89, 203, 80))))
    }
}

enum class Sorting(
    override val choiceName: String,
    val comparator: Comparator<PlayerListEntry>
) : NamedChoice {
    VANILLA("Vanilla", Comparator.comparingInt { -it.listOrder }),
    PING("Ping", Comparator.comparingInt { it.latency }),
    LENGTH("NameLength", Comparator.comparingInt { it.profile.name.length }),
    ALPHABETICAL("Alphabetical", Comparator.comparing { it.profile.name }),
    REVERSE_ALPHABETICAL("ReverseAlphabetical", Comparator.comparing({ it.profile.name }, Comparator.reverseOrder())),
    NONE("None", { _, _ -> 0 })
}


