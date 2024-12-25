package net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.translate.providers.google

interface ResponseData {
    val src: String
    val sentences: List<Sentences>
}
interface Sentences {
    // If you have any relations with this field and genders, please search the nearest cliff on DuckDuckGo
    val trans: String;
}
