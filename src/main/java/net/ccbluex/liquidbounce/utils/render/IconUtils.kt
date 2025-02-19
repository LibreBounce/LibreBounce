/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import net.ccbluex.liquidbounce.utils.io.flipSafely
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.Display
import java.awt.Image
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JFrame

@SideOnly(Side.CLIENT)
object IconUtils {

    @JvmStatic
    fun JFrame.initClientIcon(): Boolean {
        favicon?.keys?.let { // Set Icon
            iconImages = it.toMutableList()
            return true
        }
        return false
    }

    @JvmStatic
    fun initLwjglIcon(): Boolean {
        favicon?.values?.toTypedArray()?.let {
            Display.setIcon(it)
            return true
        }
        return false
    }

    private val favicon by lazy {
        IconUtils::class.java.runCatching {
            val name = CLIENT_NAME.lowercase()
            arrayOf(
                readImageToBuffer(getResourceAsStream("/assets/minecraft/$name/icon_16x16.png")),
                readImageToBuffer(getResourceAsStream("/assets/minecraft/$name/icon_32x32.png")),
                readImageToBuffer(getResourceAsStream("/assets/minecraft/$name/icon_64x64.png"))
            ).filterNotNull().toMap()
        }.onFailure {
            ClientUtils.LOGGER.warn("Failed to load icons", it)
        }.getOrNull()
    }

    @Throws(IOException::class)
    private fun readImageToBuffer(imageStream: InputStream?): Pair<Image, ByteBuffer>? {
        val bufferedImage = imageStream?.use(ImageIO::read) ?: return null
        val rgb = bufferedImage.getRGB(0, 0, bufferedImage.width, bufferedImage.height, null, 0, bufferedImage.width)
        val byteBuffer = ByteBuffer.allocate(4 * rgb.size)

        for (i in rgb) {
            byteBuffer.putInt(i shl 8 or (i ushr 24 and 255))
        }

        byteBuffer.flipSafely()
        return bufferedImage to byteBuffer
    }
}
