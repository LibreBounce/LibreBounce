/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.minecraft.util.ResourceLocation
import javax.swing.Icon
import javax.swing.ImageIcon

enum class Category(val displayName: String) {

    COMBAT("Combat"),
    PLAYER("Player"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    WORLD("World"),
    MISC("Misc"),
    EXPLOIT("Exploit"),
    FUN("Fun");

    /**
     * For Minecraft
     */
    val iconResourceLocation: ResourceLocation = ResourceLocation("${CLIENT_NAME.lowercase()}/tabgui/${name.lowercase()}.png")

    /**
     * For Swing
     */
    val icon: Icon = ImageIcon(this::class.java.getResource("/assets/minecraft/${CLIENT_NAME.lowercase()}/tabgui/${name.lowercase()}.png"), displayName)

}
