package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers

interface Provider {
    fun translate(text: String, to: String, from: String?): String
}
