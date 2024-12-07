package net.ccbluex.liquidbounce.utils.client

import net.minecraft.client.render.Fog
import net.minecraft.client.render.FogShape

fun Fog.copy(
    start: Float = this.start,
    end: Float = this.end,
    shape: FogShape = this.shape,
    red: Float = this.red,
    blue: Float = this.blue,
    green: Float = this.green, alpha: Float = this.alpha
): Fog {
    return Fog(
        start, end,
        shape, red,
        blue, green, alpha)
}
