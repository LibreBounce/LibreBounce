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
    var i = 0
    
    while (i < length) {
        val char = this[i]
        
        // Add current character
        result.append(char)
        
        // Special handling for lowercase to uppercase transition
        if (i > 0 && this[i - 1].isLowerCase() && char.isUpperCase()) {
            result.insert(result.length - 1, ' ')
        }
        
        // Special handling for lowercase to number transition
        if (i > 0 && this[i - 1].isLowerCase() && char.isDigit()) {
            result.insert(result.length - 1, ' ')
        }
        
        // Special handling for number to uppercase transition
        if (i > 0 && this[i - 1].isDigit() && char.isUpperCase()) {
            result.insert(result.length - 1, ' ')
        }
        
        // Special handling for uppercase sequences
        if (char.isUpperCase()) {
            // Check if entire string is uppercase
            var allUppercase = true
            for (j in 0 until length) {
                if (!this[j].isUpperCase()) {
                    allUppercase = false
                    break
                }
            }
            
            // If all uppercase, add space every 3 letters
            if (allUppercase && (i + 1) % 3 == 0 && i < length - 1) {
                result.insert(result.length, ' ')
            }
            
            // For mixed sequences with multiple uppercase letters
            // Add space between second-last and last uppercase letter
            else {
                var j = i + 1
                var uppercaseCount = 1
                while (j < length && this[j].isUpperCase()) {
                    uppercaseCount++
                    j++
                }
                
                if (uppercaseCount > 1 && !allUppercase && i == j - 2 && j < length) {
                    result.insert(result.length, ' ')
                }
            }
        }
        
        i++
    }
    
    return result.toString()
}
