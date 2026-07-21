/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.nbt.*

inline operator fun NbtCompound.set(key: String, value: Byte) {
    setByte(key, value)
}

inline operator fun NbtCompound.set(key: String, value: Short) {
    setShort(key, value)
}

inline operator fun NbtCompound.set(key: String, value: Int) {
    setInteger(key, value)
}

inline operator fun NbtCompound.set(key: String, value: Long) {
    setLong(key, value)
}

inline operator fun NbtCompound.set(key: String, value: Float) {
    setFloat(key, value)
}

inline operator fun NbtCompound.set(key: String, value: Double) {
    setDouble(key, value)
}

inline operator fun NbtCompound.set(key: String, value: String) {
    setString(key, value)
}

inline operator fun NbtCompound.set(key: String, value: Boolean) {
    setBoolean(key, value)
}

inline operator fun NbtCompound.set(key: String, value: ByteArray) {
    setByteArray(key, value)
}

inline operator fun NbtCompound.set(key: String, value: IntArray) {
    setIntArray(key, value)
}

inline operator fun NbtCompound.set(key: String, value: NbtCompound) {
    setTag(key, value)
}

inline operator fun NbtCompound.set(key: String, value: NbtList) {
    setTag(key, value)
}

inline fun NbtCompound(builderAction: NbtCompound.() -> Unit): NbtCompound {
    return NbtCompound().apply(builderAction)
}

inline fun NbtList(builderAction: NbtList.() -> Unit): NbtList {
    return NbtList().apply(builderAction)
}

inline fun NbtList.appendTag(builderAction: NbtCompound.() -> Unit) {
    appendTag(NbtCompound().apply(builderAction))
}

