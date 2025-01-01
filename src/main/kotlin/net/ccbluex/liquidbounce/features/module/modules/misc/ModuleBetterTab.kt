package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.text.Text

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
            PlayerHider
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
        val nameOnly by boolean("NameOnly", false)
    }

    object Highlight : ToggleableConfigurable(ModuleBetterTab, "Highlight", true) {
        open class HighlightColored(
            name: String,
            color: Color4b
        ) : ToggleableConfigurable(this, name, true) {
            val color by color("Color", color)
        }

        class Others(color: Color4b) : HighlightColored("Others", color) {
            val filter = tree(PlayerFilter())
        }

        val self = tree(HighlightColored("Self", Color4b(50, 193, 50, 80)))
        val friends = tree(HighlightColored("Friends", Color4b(16, 89, 203, 80)))
        val others = tree(Others(Color4b(35, 35, 35, 80)))
    }

    object AccurateLatency : ToggleableConfigurable(ModuleBetterTab, "AccurateLatency", true) {
        val suffix by boolean("AppendMSSuffix", true)
    }

    object PlayerHider : ToggleableConfigurable(ModuleBetterTab, "PlayerHider", false) {
        val filter = tree(PlayerFilter())
    }
}

class PlayerFilter: Configurable("Filter") {
    private var filters = setOf<Regex>()

    private val filterType by enumChoice("FilterBy", Filter.BOTH)

    @Suppress("unused")
    private val names by textArray("Names", mutableListOf()).onChanged { newValue ->
        filters = newValue.mapTo(HashSet(newValue.size, 1.0F)) {
            val regexPattern = it
                .replace("*", ".*")
                .replace("?", ".")

            Regex("^$regexPattern\$")
        }
    }

    fun isInFilter(entry: PlayerListEntry) = filters.any { regex ->
        filterType.matches(entry, regex)
    }

    @Suppress("unused")
    private enum class Filter(
        override val choiceName: String,
        val matches: PlayerListEntry.(Regex) -> Boolean
    ) : NamedChoice {
        BOTH("Both", { regex ->
            DISPLAY_NAME.matches(this, regex) || PLAYER_NAME.matches(this, regex)
        }),

        DISPLAY_NAME("DisplayName", { regex ->
            this.displayName?.string?.let { regex.matches(it) } ?: false
        }),

        PLAYER_NAME("PlayerName", { regex ->
            regex.matches(this.profile.name)
        })
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
    SCORE_LENGTH("DisplayNameLength", Comparator.comparingInt { (it.displayName ?: Text.empty()).string.length }),
    ALPHABETICAL("Alphabetical", Comparator.comparing { it.profile.name }),
    REVERSE_ALPHABETICAL("ReverseAlphabetical", Comparator.comparing({ it.profile.name }, Comparator.reverseOrder())),
    NONE("None", { _, _ -> 0 })
}


