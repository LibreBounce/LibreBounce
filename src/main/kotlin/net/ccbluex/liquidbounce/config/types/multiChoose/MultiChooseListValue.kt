package net.ccbluex.liquidbounce.config.types.multiChoose

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.config.types.ListValueType
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import java.util.*

sealed class MultiChooseListValue<T>(
    name: String,
    value: AbstractSet<T>,
    @Exclude val choices: AbstractSet<T>,

    /**
     * Can deselect all values or enable at least one
     */
    @Exclude val canBeNone: Boolean = true,
    listType: ListValueType,

    /**
     * If the [AbstractSet] automatically implements sorting and guarantees order,
     * then set the [autoSorting] to true.
     * Otherwise, if the insertion order is not guaranteed,
     * leave [autoSorting] to false and then the implementation guarantees the order.
     */
    @Exclude @ProtocolExclude private val autoSorting: Boolean
) : Value<AbstractSet<T>>(
    name,
    defaultValue = value,
    valueType = ValueType.MULTI_CHOOSE,
    listType = listType
) {
    init {
        if (!canBeNone) {
            require(choices.isNotEmpty()) {
                "There are no values provided, " +
                    "but at least one must be selected. (required because by canBeNone = false)"
            }

            require(value.isNotEmpty()) {
                "There are no default values enabled, " +
                    "but at least one must be selected. (required because by canBeNone = false)"
            }
        }

        val extra = HashSet(value)
        extra.removeAll(choices)

        require(extra.isEmpty()) {
            "Value contains extra elements not present in choices: $extra"
        }
    }

    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val active = get()
        active.clear()

        when (element) {
            is JsonArray -> element.forEach { active.tryToEnable(it.asString) }
            is JsonPrimitive -> active.tryToEnable(element.asString)
        }

        if (!canBeNone && active.isEmpty()) {
            active.addAll(choices)
        } else if (!autoSorting) {
            val temp = LinkedHashSet(active)
            active.clear()

            for (choice in choices) {
                if (temp.contains(choice)) {
                    active.add(choice)
                }
            }
        }

        set(active)
    }

    private fun MutableSet<T>.tryToEnable(name: String) {
        val choiceWithName = choices.firstOrNull { it.elementName == name }

        if (choiceWithName != null) {
            add(choiceWithName)
        }
    }

    fun toggle(value: T): Boolean {
        val current = get()

        val isActive = value in current

        if (isActive) {
            if (!canBeNone && current.size <= 1) {
                return true
            }

            current.remove(value)
        } else {
            current.add(value)
        }

        set(current)

        return !isActive
    }

    protected abstract val T.elementName: String

    operator fun contains(choice: T) = get().contains(choice)
}
