/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

fun String.toLowerCamelCase() = String(toCharArray().apply {
    this[0] = this[0].lowercaseChar()
})

fun String.sentenceCase() =
    split(" ").mapIndexed { index, word ->
        when {
            index == 0 || word.all { it.isUpperCase() } -> word
            else -> word.replaceFirstChar { it.lowercase() }
        }
    }.joinToString(" ")

fun String.capitalize(mode: String) =
    when (mode) {
        "Uppercase" -> this.uppercase()
        "Lowercase" -> this.lowercase()
        "Sentence" -> this.sentenceCase()
        else -> this
    }

fun String.addSpaces(addSpaces: Boolean = true): String {
    if (!addSpaces) return this

    val result = StringBuilder()
    var i = 0
    
    while (i < length) {
        val char = this[i]
        
        result.append(char)
        
        if (i > 0) {
        	when {
            	this[i - 1].isLowerCase() && (char.isUpperCase() || char.isDigit()) -> result.insert(result.length - 1, ' ')
                (this[i - 1].isDigit() && char.isLetter()) || (char.isDigit() && this[i - 1].isLetter()) -> result.insert(result.length - 1, ' ')
            }
        }
        
        if (char.isUpperCase()) {
            var allUppercase = true

            for (j in 0 until length) {
                if (this[j].isLowerCase()) {
                    allUppercase = false
                    break
                }
            }

            var j = i + 1

            if (allUppercase && j % 3 == 0 && i < length - 1) {
                result.insert(result.length, ' ')
            } else {
                var uppercaseCount = 1

                while (j < length && this[j].isUpperCase()) {
                    uppercaseCount++
                    j++
                }
                
                if (uppercaseCount > 1 && !allUppercase &&
                        i == j - 2 && j + 1 < length && this[j + 1].isLowerCase()
                   ) {
                    result.insert(result.length, ' ')
                }
            }
        }
        
        i++
    }
    
    return result.toString()
}
