package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ElytraItem
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket

/**
Elytra recast module

Recasts elytra while holding jump key.
 */


object ModuleElytraRecast : Module("ElytraRecast", Category.MOVEMENT) {

    init {
        enableLock()
    }

    fun recastElytra(player: ClientPlayerEntity): Boolean {
        if (checkElytra(player) && checkFallFlyingIgnoreGround(player)) {
            network.sendPacket(
                ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING
                ))
            return true
        }
        return false
    }
    fun checkElytra(player: ClientPlayerEntity): Boolean {
        val itemStack = player.getEquippedStack(EquipmentSlot.CHEST)
        return (!player.abilities.flying && !player.hasVehicle()
            && !player.isClimbing && itemStack.isOf(Items.ELYTRA)
            && ElytraItem.isUsable(itemStack) && player.input.jumping)
    }

    fun checkFallFlyingIgnoreGround(player: ClientPlayerEntity): Boolean {
        if (!player.isTouchingWater && !player.hasStatusEffect(StatusEffects.LEVITATION)) {
            val itemStack = player.getEquippedStack(EquipmentSlot.CHEST)
            if (itemStack.isOf(Items.ELYTRA) && ElytraItem.isUsable(itemStack)) {
                player.startFallFlying()
                return true
            }
        }
        return false
    }
}
