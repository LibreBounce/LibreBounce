/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

fun String.toLowerCamelCase() = String(toCharArray().apply {
    this[0] = this[0].lowercaseChar()
})

fun String.addSpaces(): String {
    val result = StringBuilder()
    var uppercaseCount = 0
    var i = 0
    
    while (i < length) {
        val char = this[i]
        
        // Add current character
        result.append(char)
        
        // Special handling for lowercase to uppercase transition
        if (i > 0 && this[i-1].isLowerCase() && char.isUpperCase()) {
            result.insert(result.length - 1, ' ')
        }
        
        // Track and handle uppercase sequences
        if (char.isUpperCase()) {
            uppercaseCount++
            
            // Add space after 3 consecutive uppercase letters
            if (uppercaseCount == 3 && i < length - 1 && this[i + 1].isUpperCase()) {
                result.append(' ')
                uppercaseCount = 0
            }
        } else {
            // Reset uppercase count for non-uppercase characters
            uppercaseCount = 0
        }
        
        i++
    }
    
    return result.toString()
}
