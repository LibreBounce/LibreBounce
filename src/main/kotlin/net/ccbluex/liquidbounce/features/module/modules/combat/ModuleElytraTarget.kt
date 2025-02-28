package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

/**
 * Initial firework cooldown
 */
@Suppress("MagicNumber")
private var fireworkCooldown = 750

private const val IDEAL_DISTANCE = 10

/**
 * Following the target on elytra.
 * Works with [ModuleKillAura] together
 *
 * https://youtu.be/1wa8uKH_apY?si=H84DmdQ2HtvArIPZ
 *
 * @author sqlerrorthing
 */
@Suppress("MagicNumber", "Unused", "UnusedPrivateProperty")
object ModuleElytraTarget : ClientModule("ElytraTarget", Category.COMBAT) {
    private val targetAt by enumChoice("TargetAt", TargetPosition.EYES)
    private val targetTracker = tree(TargetTracker())

    private val look by boolean("Look", false)
    private val smartHeight by boolean("SmartHeight", false)
    private val predict by boolean("PredictMovement", true)
    private val autoDistance by boolean("AutoDistance", true)

    private val autoFirework = tree(object : ToggleableConfigurable(this, "AutoFirework", true) {
        val useMode by enumChoice("UseMode", FireworkUseMode.PACKET)
        val extraDistance by float("ExtraDistance", 50f, 5f..100f, suffix = "m")
        val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..20, "ticks")
        val cooldown by intRange("Cooldown", 8..10, 1..50, "ticks")
    })

    override val running: Boolean
        get() = super.running && player.isGliding

    private inline val fireworkSlot
        get() = if (OffHandSlot.itemStack.item == Items.FIREWORK_ROCKET) {
            OffHandSlot
        } else {
            Slots.Hotbar.findSlot(Items.FIREWORK_ROCKET)
        }

    private inline val randomDirectionVector
        get() = with (System.currentTimeMillis() / 1000.0) {
            Vec3d(
                sin(this * 1.8) * 0.04 + (Math.random() - 0.5) * 0.02,
                sin(this * 2.2) * 0.03 + (Math.random() - 0.5) * 0.015,
                cos(this * 1.8) * 0.04 + (Math.random() - 0.5) * 0.02,
            )
        }

    private val fireworkChronometer = Chronometer()

    @Suppress("unused")
    private val targetUpdate = handler<RotationUpdateEvent> {
        for (target in targetTracker.targets()) {
            val correction = if (look) {
                MovementCorrection.CHANGE_LOOK
            } else {
                MovementCorrection.STRICT
            }

            targetTracker.target = target
            calculateRotation(target).let {
                RotationManager.setRotationTarget(
                    /*
                     * Don't use the RotationConfigurable because I need to superfast rotations.
                     * Without any setting and angle smoothing
                     */
                    plan = RotationTarget(
                        rotation = it,
                        vec3d = it.directionVector,
                        entity = target,
                        angleSmooth = null,
                        slowStart = null,
                        failFocus = null,
                        shortStop = null,
                        ticksUntilReset = 1,
                        resetThreshold = 1f,
                        considerInventory = true,
                        movementCorrection = correction
                    ),
                    priority = Priority.IMPORTANT_FOR_USAGE_3,
                    provider = this
                )
            }

            return@handler
        }

        targetTracker.reset()
    }

    private fun calculateRotation(target: LivingEntity): Rotation {
        var targetPos = if (predict) {
            targetAt.position(target).add(target.velocity * 2.0)
        } else {
            targetAt.position(target)
        } + randomDirectionVector * 4.0

        if (smartHeight) {
            (player.y - target.y).takeIf { it < 5 }?.let {
                targetPos = targetPos.add(0.0, 5 - it, 0.0)
            }
        }

        if (autoDistance) {
            val direction = (targetPos - player.pos).normalize()
            val distance = player.pos.squaredDistanceTo(direction)

            if (distance < IDEAL_DISTANCE * IDEAL_DISTANCE) {
                targetPos -= direction * (IDEAL_DISTANCE - distance)
            }
        }

        return Rotation.lookingAt(targetPos, player.pos)
    }

    @Suppress("unused")
    private val autoFireworkHandler = tickHandler {
        val target = targetTracker.target
            ?.takeIf { autoFirework.enabled } ?: return@tickHandler

        if (fireworkChronometer.hasElapsed((fireworkCooldown * 50).toLong())) {
            val slot = fireworkSlot ?: return@tickHandler
            autoFirework.useMode.useFireworkSlot(slot, autoFirework.slotResetDelay.random())
            fireworkChronometer.reset()
        }

        val distance = autoFirework.extraDistance

        fireworkCooldown = if (target.squaredBoxedDistanceTo(player) > distance * distance) {
            autoFirework.cooldown.max()
        } else {
            autoFirework.cooldown.min()
        }
    }

    override fun disable() {
        targetTracker.reset()
    }

    private enum class FireworkUseMode(
        override val choiceName: String,
        val useFireworkSlot: (HotbarItemSlot, Int) -> Unit
    ) : NamedChoice {
        NORMAL("Normal", { slot, resetDelay ->
            useHotbarSlotOrOffhand(slot, resetDelay)
        }),
        PACKET("Packet", { slot, _ ->
            with (player.inventory.selectedSlot) {
                val slotUpdateFlag = slot !is OffHandSlot && slot.hotbarSlotForServer != this

                if (slotUpdateFlag) {
                    player.inventory.selectedSlot = slot.hotbarSlotForServer
                    network.sendPacket(UpdateSelectedSlotC2SPacket(slot.hotbarSlotForServer))
                }

                interaction.sendSequencedPacket(world) { sequence ->
                    PlayerInteractItemC2SPacket(slot.useHand, sequence, player.yaw, player.pitch)
                }

                if (slotUpdateFlag) {
                    player.inventory.selectedSlot = this
                    network.sendPacket(UpdateSelectedSlotC2SPacket(slot.hotbarSlotForServer))
                }
            }
        })
    }

    @Suppress("unused")
    private enum class TargetPosition(
        override val choiceName: String,
        val position: (LivingEntity) -> Vec3d
    ) : NamedChoice {
        EYES("Eyes", { target ->
            target.eyePos
        }),
        CENTER("Center", { target ->
            target.pos.add(0.0, target.height / 2.0, 0.0)
        })
    }
}
