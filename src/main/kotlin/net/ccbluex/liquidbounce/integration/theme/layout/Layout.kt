package net.ccbluex.liquidbounce.integration.theme.layout

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.theme.type.Theme

class Layout(theme: Theme) : Choice(
    theme.name,
    value = mutableListOf(
        *theme.components
            .filter { factory -> factory.default }
            .map { factory -> factory.createComponent(theme) }.toTypedArray()
    )
) {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleHud.layouts
}
