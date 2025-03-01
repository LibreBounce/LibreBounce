/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.client

object ClassUtils {

    private val cachedClasses = mutableMapOf<String, Boolean>()

    /**
     * Allows you to check for existing classes with the [className]
     */
    fun hasClass(className: String) =
        if (className in cachedClasses)
            cachedClasses[className]!!
        else try {
            Class.forName(className)
            cachedClasses[className] = true

            true
        } catch (e: ClassNotFoundException) {
            cachedClasses[className] = false

            false
        }

    fun hasForge() = hasClass("net.minecraftforge.common.MinecraftForge")

}

val Any.debugString: String
    get() = this::class.java.declaredFields.joinToString(
        separator = ", ",
        prefix = "${this::class.java.simpleName}(",
        postfix = ")"
    ) { property ->
        property.isAccessible = true
        val name = property.name
        val value = property.get(this)
        "$name=$value"
    }
