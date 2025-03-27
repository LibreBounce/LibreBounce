package net.ccbluex.liquidbounce.config.types.multiChoose

import net.ccbluex.liquidbounce.config.types.ListValueType
import net.ccbluex.liquidbounce.config.types.NamedChoice
import java.util.*

class MultiChooseEnumListValue<T>(
    name: String,
    value: EnumSet<T>,
    choices: EnumSet<T>,
    canBeNone: Boolean = true,
) : MultiChooseListValue<T>(
    name,
    value = value,
    choices = choices,
    canBeNone = canBeNone,
    listType = ListValueType.Enums
) where T : Enum<T>, T : NamedChoice {
    override val T.elementName: String
        get() = choiceName
}
