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
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.AutoConfig.loadingNow
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.types.*
import net.ccbluex.liquidbounce.event.EventHandler
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.util.InputUtil

/**
 * A module also called 'hack' can be enabled and handle events
 */
@Suppress("LongParameterList")
open class ClientModule(
    name: String, // name parameter in configurable
    @Exclude val category: Category, // module category
    bind: Int = InputUtil.UNKNOWN_KEY.code, // default bind
    bindAction: InputBind.BindAction = InputBind.BindAction.TOGGLE, // default action
    state: Boolean = false, // default state
    @Exclude val disableActivation: Boolean = false, // disable activation
    hide: Boolean = false, // default hide
    @Exclude val disableOnQuit: Boolean = false, // disables module when player leaves the world,
    @Exclude val aliases: Array<out String> = emptyArray() // additional names under which the module is known
) : EventHandler, Configurable(name), MinecraftShortcuts {

    /**
     * Option to enable or disable the module, this DOES NOT mean the module is running. This
     * should be checked with [running] instead. Only use this for toggling the module!
     */
    var enabled by boolean("Enabled", state).also {
        // Might not include the enabled state of the module depending on the category
        if (category == Category.MISC || category == Category.FUN || category == Category.RENDER) {
            if (this is ModuleAntiBot) {
                return@also
            }

            it.doNotIncludeAlways()
        }
    }.notAnOption().onChange { new ->
        // Check if the module is locked
        locked?.let { locked ->
            if (locked.get()) {
                notification(
                    this.name,
                    translation("liquidbounce.generic.locked"),
                    NotificationEvent.Severity.ERROR
                )

                // Keeps it turned off
                return@onChange false
            }
        }

        runCatching {
            if (!inGame) {
                return@runCatching
            }

            calledSinceStartup = true

            // Call enable or disable function
            if (new) {
                enable()
            } else {
                disable()
            }
        }.onSuccess {
            // Notify everyone about module activation
            EventManager.callEvent(ModuleActivationEvent(name))

            // Save new module state when module activation is enabled
            if (disableActivation) {
                return@onChange false
            }

            if (!loadingNow) {
                notification(
                    if (new) translation("liquidbounce.generic.enabled")
                    else translation("liquidbounce.generic.disabled"),
                    this.name,
                    if (new) NotificationEvent.Severity.ENABLED else NotificationEvent.Severity.DISABLED
                )
            }

            // Notify everyone about module state
            EventManager.callEvent(ModuleToggleEvent(name, hidden, new))

            // Call to state-aware sub-configurables
            inner.filterIsInstance<ChoiceConfigurable<*>>().forEach { it.newState(new) }
            inner.filterIsInstance<ToggleableConfigurable>().forEach { it.newState(new) }
        }.onFailure {
            // Log error
            logger.error("Module failed to ${if (new) "enable" else "disable"}.", it)
            // In case of an error, module should stay disabled
            throw it
        }

        new
    }

    /**
     * If the module is running and in game. Can be overridden to add additional checks.
     */
    override val running: Boolean
        get() = super.running && inGame && enabled

    val bind by bind("Bind", InputBind(InputUtil.Type.KEYSYM, bind, bindAction))
        .doNotIncludeWhen { !AutoConfig.includeConfiguration.includeBinds }
        .independentDescription()
    var hidden by boolean("Hidden", hide)
        .doNotIncludeWhen { !AutoConfig.includeConfiguration.includeHidden }
        .independentDescription()
        .onChange {
            EventManager.callEvent(RefreshArrayListEvent())
            it
        }

    /**
     * If this value is on true, we cannot enable the module, as it likely does not bypass.
     */
    private var locked: Value<Boolean>? = null

    override val baseKey: String
        get() = "liquidbounce.module.${name.toLowerCamelCase()}"

    // Tag to be displayed on the HUD
    open val tag: String?
        get() = this.tagValue?.getValue()?.toString()

    private var tagValue: Value<*>? = null

    /**
     * Allows the user to access values by typing module.settings.<valuename>
     */
    @ScriptApiRequired
    open val settings by lazy { inner.associateBy { it.name } }

    private var calledSinceStartup = false

    /**
     * Called when module is turned on
     */
    open fun enable() {}

    /**
     * Called when module is turned off
     */
    open fun disable() {}

    /**
     * Called when the module is added to the module manager
     */
    open fun init() {}

    /**
     * Handles disconnect and if [disableOnQuit] is true disables module
     */
    @Suppress("unused")
    val onDisconnect = handler<DisconnectEvent>(ignoreNotRunning = true) {
        if (enabled && disableOnQuit) {
            enabled = false
        }
    }

    @Suppress("unused")
    val onWorldChange = handler<WorldChangeEvent>(ignoreNotRunning = true) {
        if (enabled && !calledSinceStartup && it.world != null) {
            calledSinceStartup = true
            enable()
        }
    }

    /**
     * If we want a module to have the requires bypass option, we specifically call it
     * on init. This will add the option and enable the feature.
     */
    fun enableLock() {
        this.locked = boolean("Locked", false)
    }

    fun tagBy(setting: Value<*>) {
        check(this.tagValue == null) { "Tag already set" }

        this.tagValue = setting

        // Refresh arraylist on tag change
        setting.onChanged {
            EventManager.callEvent(RefreshArrayListEvent())
        }
    }

    /**
     * Warns when no module description is set in the main translation file.
     *
     * Requires that [Configurable.walkKeyPath] has previously been run.
     */
    fun verifyFallbackDescription() {
        if (!LanguageManager.hasFallbackTranslation(descriptionKey!!)) {
            logger.warn("$name is missing fallback description key $descriptionKey")
        }
    }

    protected fun <T: Choice> choices(name: String, active: T, choices: Array<T>) =
        choices(this, name, active, choices)

    protected fun <T : Choice> choices(
        name: String,
        activeIndex: Int,
        choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
    ) = choices(this, name, { it.choices[activeIndex] }, choicesCallback)

    fun message(key: String, vararg args: Any) = translation("$baseKey.messages.$key", *args)

}
