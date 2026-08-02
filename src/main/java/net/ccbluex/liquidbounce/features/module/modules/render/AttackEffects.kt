package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.block.Block
import net.minecraft.entity.living.LivingEntity
import net.minecraft.entity.effect.EntityLightningBolt
import net.minecraft.init.Blocks.redstone_block
import net.minecraft.network.packet.s2c.play.AddGlobalEntityS2CPacket
import net.minecraft.entity.particle.ParticleType

object AttackEffects : Module("AttackEffects", Category.RENDER) {

    private val particle by choices(
        "Particle",
        arrayOf("None", "Blood", "Lighting", "Fire", "Heart", "Water", "Smoke", "Magic", "Crits"), "Blood"
    )

    private val amount by int("ParticleAmount", 5, 1..20) { particle != "None" }

    private val sound by choices("Sound", arrayOf("None", "Hit", "Orb", "Pop", "Splash", "Lightning"), "BowHit")

    private val volume by float("Volume", 1f, 0.1f..5f) { sound != "None" }
    private val pitch by float("Pitch", 1f, 0.1f..5f) { sound != "None" }

    val onAttack = handler<AttackEvent> { event ->
        val target = event.targetEntity as? LivingEntity ?: return@handler

        repeat(amount) {
            doEffect(target)
        }

        doSound()
    }

    private fun doSound() {
        val player = mc.player

        when (sound) {
            "Hit" -> player.playSound("random.bowhit", volume, pitch)
            "Orb" -> player.playSound("random.orb", volume, pitch)
            "Pop" -> player.playSound("random.pop", volume, pitch)
            "Splash" -> player.playSound("random.splash", volume, pitch)
            "Lightning" -> player.playSound("ambient.weather.thunder", volume, pitch)
        }
    }

    private fun doEffect(target: LivingEntity) {
        when (particle) {
            "Blood" -> spawnBloodParticle(ParticleType.BLOCK_CRACK, target)
            "Crits" -> spawnEffectParticle(ParticleType.CRIT, target)
            "Magic" -> spawnEffectParticle(ParticleType.CRIT_MAGIC, target)
            "Lighting" -> spawnLightning(target)
            "Smoke" -> spawnEffectParticle(ParticleType.SMOKE_NORMAL, target)
            "Water" -> spawnEffectParticle(ParticleType.WATER_DROP, target)
            "Heart" -> spawnEffectParticle(ParticleType.HEART, target)
            "Fire" -> spawnEffectParticle(ParticleType.LAVA, target)
        }
    }

    private fun spawnBloodParticle(particleType: ParticleType, target: LivingEntity) {
        mc.world.spawnParticle(
            particleType,
            target.x, target.y + target.height - 0.75, target.z,
            0.0, 0.0, 0.0,
            Block.getStateId(redstone_block.defaultState)
        )
    }

    private fun spawnEffectParticle(particleType: ParticleType, target: LivingEntity) {
        mc.particleManager.spawnEffectParticle(
            particleType.id,
            target.x, target.y, target.z,
            target.x, target.y, target.z
        )
    }

    private fun spawnLightning(target: LivingEntity) {
        mc.networkHandler.handleSpawnGlobalEntity(
            AddGlobalEntityS2CPacket(
                EntityLightningBolt(mc.world, target.x, target.y, target.z)
            )
        )
    }

}