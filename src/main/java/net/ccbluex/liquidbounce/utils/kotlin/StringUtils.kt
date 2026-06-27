/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.kotlin

import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@SideOnly(Side.CLIENT)
object StringUtils {
    fun toCompleteString(args: Array<String>, start: Int) =
        if (args.size <= start) ""
        else args.drop(start).joinToString(separator = " ")

    /**
     * Checks if a nullable String converted to lowercase contains any of the given lowercase substrings.
     * It returns true if at least one substring is found, false otherwise.
     * @param substrings an array of Strings to look for in the nullable String
     * @return true if any substring is found, false otherwise
     */
    operator fun String?.contains(substrings: Array<String>): Boolean {
        val lowerCaseString = this?.lowercase() ?: return false
        return substrings.any { it in lowerCaseString }
    }
}
