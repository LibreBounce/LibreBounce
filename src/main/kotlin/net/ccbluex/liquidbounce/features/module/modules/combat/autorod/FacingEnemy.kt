package net.ccbluex.liquidbounce.features.module.modules.combat.autorod

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.aiming.utils.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceEntity
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity

@Suppress("MagicNumber")
internal class FacingEnemy : ToggleableConfigurable(ModuleAutoRod, "FacingEnemy", true) {
    private val activationDistance by floatRange("ActivationDistance", 8f..8f, 1f..20f, suffix = "m")
    private val enemiesNearby by int("EnemiesNearby", 1, 1..5)
    private val ignoreOnLowHealth by boolean("IgnoreOnLowHealth", true)
    private val healthThreshold by int("HealthThreshold", 10, 1..20)

    private inline val nearbyEnemies: List<Entity>
        get() = world.entities.filter {
            it.shouldBeAttacked() && it.pos.squaredDistanceTo(player.pos) <= activationDistance.random()
        }

    @Suppress("ReturnCount")
    internal fun testUseRod(): Boolean {
        val facingEntity = raytraceEntity(activationDistance.random().toDouble(), player.rotation) {
            it is PlayerEntity && it.shouldBeAttacked()
        }?.entity as? PlayerEntity ?: return false

        val facesEnemy = facingEnemy(
            toEntity = facingEntity, rotation = player.rotation, range = activationDistance.random().toDouble(),
            wallsRange = 0.0
        )

        if (facesEnemy && nearbyEnemies.size <= enemiesNearby) {
            if (ignoreOnLowHealth) {
                if (facingEntity.health >= healthThreshold) {
                    return true
                }
            } else {
                return true
            }
        }

        return false
    }
}
