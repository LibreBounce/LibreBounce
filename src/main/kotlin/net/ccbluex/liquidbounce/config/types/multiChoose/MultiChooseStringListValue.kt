package net.ccbluex.liquidbounce.config.types.multiChoose

import net.ccbluex.liquidbounce.config.types.ListValueType
import java.util.*


class MultiChooseStringListValue(
    name: String,
    value: AbstractSet<String>,
    choices: AbstractSet<String>,
    canBeNone: Boolean = true,
) : MultiChooseListValue<String>(
    name,
    value = value,
    choices = choices,
    canBeNone = canBeNone,
    listType = ListValueType.String
) {
    override val String.elementName: String
        get() = this
}
