/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.config

import com.google.gson.JsonElement
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private typealias OnChangeInterceptor<T> = (old: T, new: T) -> T
private typealias OnChangedHandler<T> = (new: T) -> Unit

sealed class Value<T>(
    val name: String,
    protected open var value: T,
    val subjective: Boolean = false,
    var isSupported: (() -> Boolean)? = null,
    val suffix: String? = null,
    protected var default: T = value,
) : ReadWriteProperty<Any?, T> {

    var excluded: Boolean = false
        private set

    var hidden = false
        private set

    fun setAndUpdateDefault(new: T): Boolean {
        default = new

        return set(new)
    }

    fun set(newValue: T, saveImmediately: Boolean = true): Boolean {
        if (newValue == value || hidden || excluded) {
            return false
        }

        val oldValue = value

        try {
            var handledValue = oldValue
            onChangeInterceptors.forEach { handledValue = it(oldValue, handledValue) }

            handledValue = onChange(oldValue, newValue) // TODO: remove this line
            if (handledValue == oldValue) {
                return false
            }

            changeValue(handledValue)
            onChangedListeners.forEach { it.invoke(handledValue) }
            onChanged(oldValue, handledValue) // TODO: remove this line

            if (saveImmediately) {
                saveConfig(valuesConfig)
            }
            return true
        } catch (e: Exception) {
            LOGGER.error("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
            return false
        }
    }

    /**
     * Use only when you want an option to be hidden while keeping its state.
     *
     * [state] the value it will be set to before it is hidden.
     */
    fun hideWithState(state: T = value) {
        setAndUpdateDefault(state)

        hidden = true
    }

    /**
     * Excludes the chosen option [value] from the config system.
     *
     * [state] the value it will be set to before it is excluded.
     */
    fun excludeWithState(state: T = value) {
        setAndUpdateDefault(state)

        excluded = true
    }

    fun get() = value

    open fun changeValue(newValue: T) {
        value = newValue
    }

    open fun toJson() = toJsonF()

    open fun fromJson(element: JsonElement) {
        val result = fromJsonF(element)
        if (result != null) changeValue(result)
    }

    abstract fun toJsonF(): JsonElement?
    abstract fun fromJsonF(element: JsonElement): T?

    private var onChangeInterceptors: Array<OnChangeInterceptor<T>> = emptyArray()
    private var onChangedListeners: Array<OnChangedHandler<T>> = emptyArray()

    fun onChange(interceptor: OnChangeInterceptor<T>): Value<T> {
        onChangeInterceptors += interceptor
        return this
    }

    fun onChanged(handler: OnChangedHandler<T>): Value<T> {
        this.onChangedListeners += handler
        return this
    }

    // TODO: START
    protected open fun onUpdate(value: T) {}
    protected open fun onChange(oldValue: T, newValue: T) = newValue
    protected open fun onChanged(oldValue: T, newValue: T) {}

    open fun isSupported() = isSupported?.invoke() != false

    open fun setSupport(condition: (Boolean) -> Boolean) {
        isSupported = { condition(isSupported()) }
    }

    // TODO: END

    // Support for delegating values using the `by` keyword.
    override operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        set(value)
    }

    open fun getString() = "$value"

    fun shouldRender() = isSupported() && !hidden

    fun reset() = set(default)
}
