package net.ccbluex.liquidbounce.utils.client

//import net.ccbluex.liquidbounce.render.engine.Color4b
import net.minecraft.client.render.Fog
import net.minecraft.client.render.FogShape

fun Fog.copy(
    positionPair: Pair<Float, Float> = Pair(this.start, this.end),
    shape: FogShape = this.shape,
    color: FloatColor = FloatColor(this.red, this.green, this.blue, this.alpha),
): Fog {
    return Fog(
        positionPair.first, positionPair.second,
        shape, color.r,
        color.g, color.b, color.a)
}

// could be merged into `Color4b` as generics, but not sure if izuna would be fine with that.
data class FloatColor(val r: Float, val g: Float, val b: Float, val a: Float) {
//    constructor(color: Color4b) : this(
//        color.r.toFloat(), color.g.toFloat(),
//        color.b.toFloat(), color.a.toFloat()
//    )
}
