/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.inventory.isEmpty
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawCircle
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.utils.rotation.RandomizationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.faceTrajectory
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.*
import java.awt.Color

object ProjectileAimbot : Module("ProjectileAimbot", Category.COMBAT) {

    private val bow by boolean("Bow", true).subjective()
    private val egg by boolean("Egg", true).subjective()
    private val snowball by boolean("Snowball", true).subjective()
    private val pearl by boolean("EnderPearl", false).subjective()
    private val otherItems by boolean("OtherItems", false).subjective()

    private val range by float("Range", 10f, 0f..30f, suffix = "blocks")
    private val throughWalls by boolean("ThroughWalls", false)
    private val throughWallsRange by float("ThroughWallsRange", 10f, 0f..30f, suffix = "blocks") { throughWalls }

    private val priority by choices(
        "Priority",
        arrayOf("Health", "Distance", "Direction"),
        "Direction"
    )

    private val gravityType by choices("GravityType", arrayOf("None", "Projectile"), "Projectile")

    private val predict by boolean("Predict", true) { gravityType == "Projectile" }
    private val predictSize by float("PredictSize", 2f, 0.1f..5f)
    { predict && gravityType == "Projectile" }

    private val options = RotationSettings(this).withoutKeepRotation()

    private val randomization = RandomizationSettings(this) { options.rotationsActive }

    private val highestBodyPointToTargetValue = choices(
        "HighestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Head"
    ) {
        options.rotationsActive
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val lowestPoint = RotationUtils.BodyPoint.fromString(lowestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, lowestPoint, RotationUtils.BodyPoint.HEAD)
        coercedPoint.displayName
    }
    private val highestBodyPointToTarget: String by highestBodyPointToTargetValue

    private val lowestBodyPointToTargetValue = choices(
        "LowestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Feet"
    ) {
        options.rotationsActive
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val highestPoint = RotationUtils.BodyPoint.fromString(highestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, RotationUtils.BodyPoint.FEET, highestPoint)
        coercedPoint.displayName
    }

    private val lowestBodyPointToTarget: String by lowestBodyPointToTargetValue

    private val horizontalBodySearchRange by floatRange("HorizontalBodySearchRange", 0f..1f, 0f..1f)
    { options.rotationsActive }

    // Visuals
    private val mark by choices("Mark", arrayOf("None", "Platform", "Box", "Circle"), "Circle").subjective()

    private val markColor by color("MarkColor", Color(37, 126, 255, 70)) { mark in arrayOf("Platform", "Box") }.subjective()

    // Circle options
    private val circleStartColor by color("CircleStartColor", Color.BLUE) { mark == "Circle" }.subjective()
    private val circleEndColor by color("CircleEndColor", Color.CYAN.withAlpha(0)) { mark == "Circle" }.subjective()
    private val fillInnerCircle by boolean("FillInnerCircle", false) { mark == "Circle" }.subjective()
    private val withHeight by boolean("WithHeight", true) { mark == "Circle" }.subjective()
    private val animateHeight by boolean("AnimateHeight", false) { withHeight }.subjective()
    private val heightRange by floatRange("HeightRange", 0.0f..0.4f, -2f..2f) { withHeight }.subjective()
    private val extraWidth by float("ExtraWidth", 0F, 0F..2F) { mark == "Circle" }.subjective()
    private val animateCircleY by boolean("AnimateCircleY", true) { fillInnerCircle || withHeight }.subjective()
    private val circleYRange by floatRange("CircleYRange", 0F..0.5F, 0F..2F) { animateCircleY }.subjective()
    private val duration by float(
        "Duration", 1.5F, 0.5F..3F, suffix = "Seconds"
    ) { animateCircleY || animateHeight }.subjective()

    // Box option
    private val boxOutline by boolean("Outline", true) { mark == "Box" }.subjective()

    private var target: Entity? = null

    override fun onDisable() {
        target = null
    }

    val onRotationUpdate = handler<RotationUpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        target = null

        val targetRotation = when (val item = player.heldItem?.item) {
            is ItemBow -> {
                if (!bow || !player.isUsingItem)
                    return@handler

                target = getTarget(throughWalls, priority)

                faceTrajectory(target ?: return@handler, predict, predictSize)
            }

            is Item -> {
                if (!otherItems && !player.heldItem.isEmpty() ||
                    (!egg && item is ItemEgg || !snowball && item is ItemSnowball || !pearl && item is ItemEnderPearl)
                )
                    return@handler

                target = getTarget(throughWalls, priority)

                faceTrajectory(target ?: return@handler, predict, predictSize, gravity = 0.03f, velocity = 0.5f)
            }

            else -> return@handler
        }

        val normalRotation = target?.entityBoundingBox?.let {
            searchCenter(
                it,
                outborder = false,
                randomization = this.randomization,
                predict = true,
                lookRange = range,
                attackRange = range,
                throughWallsRange = throughWallsRange,
                bodyPoints = listOf(highestBodyPointToTarget, lowestBodyPointToTarget),
                horizontalSearch = horizontalBodySearchRange
            )
        } ?: return@handler

        setTargetRotation(
            if (gravityType == "Projectile") targetRotation else normalRotation,
            options = options
        )
    }

    val onRender3D = handler<Render3DEvent> {
        target ?: return@handler

        when (mark.lowercase()) {
            "none" -> return@handler
            "platform" -> drawPlatform(target!!, markColor)
            "box" -> drawEntityBox(target!!, markColor, boxOutline)
            "circle" -> drawCircle(
                target!!,
                duration * 1000F,
                heightRange.takeIf { animateHeight } ?: heightRange.endInclusive..heightRange.endInclusive,
                extraWidth,
                fillInnerCircle,
                withHeight,
                circleYRange.takeIf { animateCircleY },
                circleStartColor.rgb,
                circleEndColor.rgb
            )
        }
    }

    private fun getTarget(throughWalls: Boolean, priorityMode: String): Entity? {
        val player = mc.thePlayer ?: return null

        return mc.theWorld.loadedEntityList
            .asSequence()
            .filterIsInstance<EntityLivingBase>()
            .filter {
                val distance = player.getDistanceToEntityBox(it)

                isSelected(it, true) && distance <= range && (throughWalls ||
                        player.canEntityBeSeen(it) && distance <= throughWallsRange)
            }.minByOrNull { entity ->
                return@minByOrNull when (priorityMode) {
                    "Distance" -> player.getDistanceToEntityBox(entity)
                    "Direction" -> rotationDifference(entity).toDouble()
                    "Health" -> entity.health.toDouble()
                    else -> 0.0 // Edge case
                }
            }
    }

    fun hasTarget() = target != null && mc.thePlayer.canEntityBeSeen(target)
}
