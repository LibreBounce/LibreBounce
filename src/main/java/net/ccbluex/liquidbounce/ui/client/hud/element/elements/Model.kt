/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.minecraft.client.render.platform.GlStateManager.*
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.render.platform.Lighting
import net.minecraft.entity.living.LivingEntity
import org.lwjgl.opengl.GL11.*
import kotlin.math.abs
import kotlin.math.atan

/**
 * CustomHUD Model element
 *
 * Draw mini figure of your character to the HUD
 */
@ElementInfo(name = "Model")
class Model(x: Double = 40.0, y: Double = 100.0) : Element("Model", x, y) {

    private val yawMode by choices("Yaw", arrayOf("Player", "Animation", "Custom"), "Animation")
    private val customYaw by float("CustomYaw", 0F, -180F..180F) { yawMode == "Custom" }

    private val pitchMode by choices("Pitch", arrayOf("Player", "Custom"), "Player")
    private val customPitch by float("CustomPitch", 0F, -90F..90F) { pitchMode == "Custom" }

    private var rotate = 0F
    private var rotateDirection = false

    /**
     * Draw element
     */
    override fun drawElement(): Border {
        val yaw = when (yawMode) {
            "Player" -> mc.player.rotationYaw
            "Animation" -> {
                val delta = deltaTime

                if (rotateDirection) {
                    if (rotate <= 70F) {
                        rotate += 0.12F * delta
                    } else {
                        rotateDirection = false
                        rotate = 70F
                    }
                } else {
                    if (rotate >= -70F) {
                        rotate -= 0.12F * delta
                    } else {
                        rotateDirection = true
                        rotate = -70F
                    }
                }

                rotate
            }

            "Custom" -> customYaw
            else -> 0F
        }

        var pitch = when (pitchMode) {
            "Player" -> mc.player.rotationPitch
            "Custom" -> customPitch
            else -> 0F
        }

        pitch = -pitch

        drawEntityOnScreen(yaw, pitch, mc.player)

        return Border(30F, 10F, -30F, -100F)
    }

    /**
     * Draw [entityLivingBase] to screen
     */
    private fun drawEntityOnScreen(yaw: Float, pitch: Float, entityLivingBase: LivingEntity) {
        resetColor()
        enableColorMaterial()
        glPushMatrix()
        glTranslatef(0F, 0F, 50F)
        glScalef(-50F, 50F, 50F)
        glRotatef(180F, 0F, 0F, 1F)

        val bodyYaw = entityLivingBase.bodyYaw
        val rotationYaw = entityLivingBase.rotationYaw
        val rotationPitch = entityLivingBase.rotationPitch
        val lastHeadYaw = entityLivingBase.lastHeadYaw
        val headYaw = entityLivingBase.headYaw

        glRotatef(135F, 0F, 1F, 0F)
        Lighting.turnOn()
        glRotatef(-135F, 0F, 1F, 0F)
        glRotatef(-atan(pitch / 40F) * 20f, 1F, 0F, 0F)

        entityLivingBase.bodyYaw = atan(yaw / 40F) * 20F
        entityLivingBase.rotationYaw = atan(yaw / 40F) * 40F
        entityLivingBase.rotationPitch = -atan(pitch / 40F) * 20F
        entityLivingBase.headYaw = entityLivingBase.rotationYaw
        entityLivingBase.lastHeadYaw = entityLivingBase.rotationYaw

        glTranslatef(0F, 0F, 0F)

        val renderManager = mc.renderManager
        renderManager.playerViewY = 180F
        renderManager.isRenderShadow = false
        renderManager.renderEntityWithPosYaw(entityLivingBase, 0.0, 0.0, 0.0, 0F, 1F)
        renderManager.isRenderShadow = true

        entityLivingBase.bodyYaw = bodyYaw
        entityLivingBase.rotationYaw = rotationYaw
        entityLivingBase.rotationPitch = rotationPitch
        entityLivingBase.lastHeadYaw = lastHeadYaw
        entityLivingBase.headYaw = headYaw

        glPopMatrix()
        Lighting.turnOff()
        disableRescaleNormal()
        setActiveTexture(OpenGlHelper.lightmapTexUnit)
        disableTexture2D()
        setActiveTexture(OpenGlHelper.defaultTexUnit)
        resetColor()
    }
}