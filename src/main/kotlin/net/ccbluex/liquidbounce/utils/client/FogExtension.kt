package net.ccbluex.liquidbounce.utils.client

import net.minecraft.client.render.Fog
import net.minecraft.client.render.FogShape

fun Fog.copy(
    positionPair: Pair<Float, Float> = Pair(this.start, this.end),
    shape: FogShape = this.shape,
    red: Float = this.red,
    blue: Float = this.blue,
    green: Float = this.green, alpha: Float = this.alpha
): Fog {
    return Fog(
        positionPair.first, positionPair.second,
        shape, red,
        blue, green, alpha)
}
