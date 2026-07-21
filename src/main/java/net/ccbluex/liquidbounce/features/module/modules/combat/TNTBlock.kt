/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.EntityLookup
import net.minecraft.client.options.GameOptions
import net.minecraft.entity.item.EntityTNTPrimed
import net.minecraft.item.SwordItem

object TNTBlock : Module("TNTBlock", Category.COMBAT) {

    private val fuse by int("Fuse", 10, 0..80)
    private val range by float("Range", 9f, 1f..20f)
    // TODO: Maybe AutoWeapon should be used for this?
    private val autoSword by boolean("AutoSword", true)
    private var blocked = false

    private val entities by EntityLookup<EntityTNTPrimed>()
        .filter { it.fuse <= fuse }
        .filter { mc.player.getSquaredDistanceToToEntity(it) <= range * range }

    val onMotion = handler<MotionEvent> {
        val player = mc.player ?: return@handler
        mc.world ?: return@handler

        for (entity in entities) {
            if (autoSword) {
                var slot = -1
                var bestDamage = 1f

                for (i in 0..8) {
                    val itemStack = player.inventory.getStackInSlot(i)

                    if (itemStack?.item is SwordItem) {
                        val itemDamage = (itemStack.item as SwordItem).damageVsEntity + 4F

                        if (itemDamage > bestDamage) {
                            bestDamage = itemDamage
                            slot = i
                        }
                    }
                }

                if (slot != -1 && slot != player.inventory.currentItem) {
                    player.inventory.currentItem = slot
                    mc.playerController.syncCurrentPlayItem()
                }
            }

            if (player.displayItemInHand?.item is SwordItem) {
                mc.gameOptions.useKey.pressed = true
                blocked = true
            }

            return@handler
        }

        if (blocked && !GameOptions.isKeyDown(mc.gameOptions.useKey)) {
            mc.gameOptions.useKey.pressed = false
            blocked = false
        }
    }
}