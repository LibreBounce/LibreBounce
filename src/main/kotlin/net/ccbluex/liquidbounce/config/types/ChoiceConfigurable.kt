/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.kotlin.mapArray

/**
 * Allows to configure and manage modes
 */
class ChoiceConfigurable<T : Choice>(
    @Exclude @ProtocolExclude val listenable: Listenable,
    name: String,
    activeChoiceCallback: (ChoiceConfigurable<T>) -> T,
    choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
) : Configurable(name, valueType = ValueType.CHOICE) {

    var choices: MutableList<T> = choicesCallback(this).toMutableList()
    private var defaultChoice: T = activeChoiceCallback(this)
    var activeChoice: T = defaultChoice

    operator fun T.unaryPlus() = choices.add(this)

    init {
        for (choice in choices) {
            choice.base = this
        }
    }

    fun newState(state: Boolean) {
        if (state) {
            this.activeChoice.enable()
        } else {
            this.activeChoice.disable()
        }

        inner.filterIsInstance<ChoiceConfigurable<*>>().forEach { it.newState(state) }
        inner.filterIsInstance<ToggleableConfigurable>().forEach { it.newState(state) }
    }

    override fun setByString(name: String) {
        val newChoice = choices.firstOrNull { it.choiceName == name }

        if (newChoice == null) {
            throw IllegalArgumentException("ChoiceConfigurable `${this.name}` has no option named $name" +
                " (available options are ${this.choices.joinToString { it.choiceName }})")
        }

        if (this.activeChoice.handleEvents()) {
            this.activeChoice.disable()
        }

        // Don't remove this! This is important. We need to call the listeners of the choice in order to update
        // the other systems accordingly. For whatever reason the conditional configurable is bypassing the value system
        // which the other configurables use, so we do it manually.
        set(mutableListOf(newChoice), apply = {
            this.activeChoice = it[0] as T
        })

        if (this.activeChoice.handleEvents()) {
            this.activeChoice.enable()
        }
    }

    override fun restore() {
        if (this.activeChoice.handleEvents()) {
            this.activeChoice.disable()
        }

        set(mutableListOf(defaultChoice), apply = {
            this.activeChoice = it[0] as T
        })

        if (this.activeChoice.handleEvents()) {
            this.activeChoice.enable()
        }
    }

    @ScriptApiRequired
    fun getChoicesStrings(): Array<String> = this.choices.mapArray { it.name }

}

/**
 * A mode is sub-module to separate different bypasses into extra classes
 */
abstract class Choice(name: String) : Configurable(name), Listenable, NamedChoice, MinecraftShortcuts {

    override val choiceName: String
        get() = this.name

    val isActive: Boolean
        get() = this.parent.activeChoice === this

    abstract val parent: ChoiceConfigurable<*>

    open fun enable() {}

    open fun disable() {}

    /**
     * We check if the parent is active and if the mode is active, if so
     * we handle the events.
     */
    override fun handleEvents() = super.handleEvents() && isActive

    override fun parent() = this.parent.listenable

    protected fun <T: Choice> choices(name: String, active: T, choices: Array<T>) =
        choices(this, name, active, choices)

    protected fun <T: Choice> choices(
        name: String,
        activeCallback: (ChoiceConfigurable<T>) -> T,
        choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
    ) = choices(this, name, activeCallback, choicesCallback)
}

/**
 * Empty choice.
 * It does nothing.
 * Use it when you want a client-user to disable a feature.
 */
class NoneChoice(override val parent: ChoiceConfigurable<*>) : Choice("None")
