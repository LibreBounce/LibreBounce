package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.minecraft.client.network.PlayerListEntry


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
            Visibility,
            Highlight,
            AccurateLatency,
        )
    }

    val sorting by enumChoice("Sorting", Sorting.VANILLA)

    object Limits : Configurable("Limits") {
        val tabSize by int("TabSize", 80, 1..1000)
        val height by int("ColumnHeight", 20, 1..100)
    }

    object Visibility : Configurable("Visibility") {
        val header by boolean("Header", true)
        val footer by boolean("Footer", true)
    }

    object Highlight : ToggleableConfigurable(ModuleBetterTab, "Highlight", true) {
        class HighlightColored(name: String, color: Color4b) : ToggleableConfigurable(this, name, true) {
            val color by color("Color", color)
        }

        val self = tree(HighlightColored("Self", Color4b(50, 193, 50, 80)))
        val friends = tree(HighlightColored("Friends", Color4b(16, 89, 203, 80)))
    }

    object AccurateLatency : ToggleableConfigurable(ModuleBetterTab, "AccurateLatency", true) {
        val suffix by boolean("AppendMSSuffix", true)
    }
}

@Suppress("unused")
enum class Sorting(
    override val choiceName: String,
    val comparator: Comparator<PlayerListEntry>?
) : NamedChoice {
    VANILLA("Vanilla", null),
    PING("Ping", Comparator.comparingInt { it.latency }),
    LENGTH("NameLength", Comparator.comparingInt { it.profile.name.length }),
    ALPHABETICAL("Alphabetical", Comparator.comparing { it.profile.name }),
    REVERSE_ALPHABETICAL("ReverseAlphabetical", Comparator.comparing({ it.profile.name }, Comparator.reverseOrder())),
    NONE("None", { _, _ -> 0 })
}


