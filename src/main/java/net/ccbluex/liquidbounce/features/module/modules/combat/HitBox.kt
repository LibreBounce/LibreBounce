/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.utils.extensions.isAnimal
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.extensions.isMob
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer

object HitBox : Module("HitBox", Category.COMBAT) {

    private val targetPlayers by boolean("TargetPlayers", true)
    private val playerSize by float("PlayerSize", 0.4f, 0f..1f) { targetPlayers }
    private val friendSize by float("FriendSize", 0.4f, 0f..1f) { targetPlayers }
    private val teamMateSize by float("TeamMateSize", 0.4f, 0f..1f) { targetPlayers }
    private val botSize by float("BotSize", 0.f, 0f..ff) { targetPlayers }

    private val targetMobs by boolean("TargetMobs", false)
    private val mobSize by float("MobSize", 0.4f, 0f..1f) { targetMobs }

    private val targetAnimals by boolean("TargetAnimals", false)
    private val animalSize by float("AnimalSize", 0.4f, 0f..1f) { targetAnimals }

    fun determineSize(entity: Entity): Float {
        return when (entity) {
            is EntityPlayer -> {
                if (entity.isSpectator || !targetPlayers) {
                    return 0f
                }

                if (isBot(entity)) {
                    return botSize
                } else if (entity.isClientFriend() && !NoFriends.handleEvents()) {
                    return friendSize
                } else if (Teams.handleEvents() && Teams.isInYourTeam(entity)) {
                    return teamMateSize
                }

                playerSize
            }

            else -> {
                if (entity.isMob() && targetMobs) {
                    return mobSize
                } else if (entity.isAnimal() && targetAnimals) {
                    return animalSize
                }

                0f
            }
        }
    }
}