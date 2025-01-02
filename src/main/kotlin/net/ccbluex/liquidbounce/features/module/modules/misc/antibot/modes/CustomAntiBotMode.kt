/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot.isADuplicate
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.item.material
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.item.Item
import net.minecraft.item.equipment.ArmorMaterial
import net.minecraft.item.equipment.ArmorMaterials
import net.minecraft.item.equipment.EquipmentType
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket
import net.minecraft.network.packet.s2c.play.EntityS2CPacket
import kotlin.math.abs

object CustomAntiBotMode : Choice("Custom"), ModuleAntiBot.IAntiBotMode {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiBot.modes

    private object InvalidGround : ToggleableConfigurable(ModuleAntiBot, "InvalidGround", true) {
        val vlToConsiderAsBot by int("VLToConsiderAsBot", 10, 1..50, "flags")
    }

    private val duplicate by boolean("Duplicate", false)
    private val noGameMode by boolean("NoGameMode", true)
    private val illegalPitch by boolean("IllegalPitch", true)
    private val fakeEntityID by boolean("FakeEntityID", true)
    private val illegalName by boolean("IllegalName", true)
    private val needHit by boolean("NeedHit", false)
    private val health by boolean("IllegalHealth", false)
    private val swung by boolean("Swung", false)
    private val critted by boolean("Critted", false)
    private val attributes by boolean("Attributes", false)

    private object AlwaysInRadius : ToggleableConfigurable(ModuleAntiBot, "AlwaysInRadius", false) {
        val alwaysInRadiusRange by float("AlwaysInRadiusRange", 20f, 5f..30f)
    }

    // LivingTime in 1.8.9
    private object Age : ToggleableConfigurable(ModuleAntiBot, "Age", false) {
        val minimum by int("Minimum", 20, 0..120, "ticks")
    }

    private object Armor : ToggleableConfigurable(ModuleAntiBot, "Armor", false) {

        @Suppress("unused")
        private enum class ArmorMaterialChoice(
            override val choiceName: String,
            val material: ArmorMaterial,
        ) : NamedChoice {
            LEATHER("Leather", ArmorMaterials.LEATHER),
            GOLD("Gold", ArmorMaterials.GOLD),
            CHAIN("Chain", ArmorMaterials.CHAIN),
            IRON("Iron", ArmorMaterials.IRON),
            DIAMOND("Diamond", ArmorMaterials.DIAMOND),
            NETHERITE("Netherite", ArmorMaterials.NETHERITE)
        }

        private class ArmorConfigurable(
            name: String, val equipmentType: EquipmentType
        ) : ToggleableConfigurable(this, name, false) {

            val mode by enumChoice("Mode", Filter.BLACKLIST)

            // TODO: replace with multiple choice
            val materialChoice by enumChoice("Material", ArmorMaterialChoice.LEATHER)

            fun isValid(item: Item): Boolean {
                with((item as? ArmorItem)?.material() == materialChoice.material) {
                    return when (mode) {
                        Filter.WHITELIST -> this
                        Filter.BLACKLIST -> !this
                    }
                }
            }
        }

        private val values = arrayOf(
            ArmorConfigurable("Boots", EquipmentType.BOOTS),
            ArmorConfigurable("Leggings", EquipmentType.LEGGINGS),
            ArmorConfigurable("Chestplate", EquipmentType.CHESTPLATE),
            ArmorConfigurable("Helmet", EquipmentType.HELMET),
        ).also { it.reversedArray().onEach(::tree) }

        fun isValid(entity: PlayerEntity): Boolean {
            return entity.armorItems.withIndex().all { (index, armor) ->
                values[index].let { !it.enabled || it.isValid(armor.item) }
            }
        }
    }

    init {
        tree(InvalidGround)
        tree(AlwaysInRadius)
        tree(Age)
        tree(Armor)
    }

    private val flyingSet = Int2IntOpenHashMap()
    private val hitListSet = IntOpenHashSet()
    private val notAlwaysInRadiusSet = IntOpenHashSet()

    private val swungSet = IntOpenHashSet()
    private val crittedSet = IntOpenHashSet()
    private val attributesSet = IntOpenHashSet()
    private val ageSet = IntOpenHashSet()

    private val armorSet = IntOpenHashSet()

    val repeatable = tickHandler {
        val rangeSquared = AlwaysInRadius.alwaysInRadiusRange.sq()
        for (entity in world.players) {
            if (player.squaredDistanceTo(entity) > rangeSquared) {
                notAlwaysInRadiusSet.add(entity.id)
            }

            if (entity.age < Age.minimum) {
                ageSet.add(entity.id)
            }

            if (Armor.enabled && !Armor.isValid(entity)) {
                armorSet.add(entity.id)
            }
        }

        with(ageSet.intIterator()) {
            while (hasNext()) {
                val entity = world.getEntityById(nextInt())
                if (entity == null || entity.age >= Age.minimum) {
                    remove()
                }
            }
        }

        with(armorSet.intIterator()) {
            while (hasNext()) {
                val entity = world.getEntityById(nextInt()) as? PlayerEntity
                if (entity == null || Armor.isValid(entity)) {
                    remove()
                }
            }
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> {
        hitListSet.add(it.entity.id)
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is EntityS2CPacket -> {
                if (!packet.isPositionChanged || !InvalidGround.enabled) {
                    return@handler
                }

                val entity = packet.getEntity(world) ?: return@handler
                val id = entity.id
                val currentValue = flyingSet.getOrDefault(id, 0)
                if (entity.isOnGround && entity.prevY != entity.y) {
                    flyingSet.put(id, currentValue + 1)
                } else if (!entity.isOnGround && currentValue > 0) {
                    val newVL = currentValue / 2

                    if (newVL <= 0) {
                        flyingSet.remove(id)
                    } else {
                        flyingSet.put(id, newVL)
                    }
                }
            }

            is EntityAttributesS2CPacket -> {
                attributesSet.add(packet.entityId)
            }

            is EntityAnimationS2CPacket -> {
                val animationId = packet.animationId

                if (animationId == EntityAnimationS2CPacket.SWING_MAIN_HAND ||
                    animationId == EntityAnimationS2CPacket.SWING_OFF_HAND) {
                    swungSet.add(packet.entityId)
                } else if (animationId == EntityAnimationS2CPacket.CRIT ||
                    animationId == EntityAnimationS2CPacket.ENCHANTED_HIT) {
                    crittedSet.add(packet.entityId)
                }
            }

            is EntitiesDestroyS2CPacket -> {
                with(packet.entityIds.intIterator()) {
                    while (hasNext()) {
                        val entityId = nextInt()
                        attributesSet.remove(entityId)
                        flyingSet.remove(entityId)
                        hitListSet.remove(entityId)
                        notAlwaysInRadiusSet.remove(entityId)
                        ageSet.remove(entityId)
                        armorSet.remove(entityId)
                    }
                }
            }
        }


    }

    private fun hasInvalidGround(player: PlayerEntity): Boolean {
        return flyingSet.getOrDefault(player.id, 0) >= InvalidGround.vlToConsiderAsBot
    }

    private const val VALID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"

    private fun hasIllegalName(player: PlayerEntity): Boolean {
        val name = player.nameForScoreboard

        if (name.length < 3 || name.length > 16) {
            return true
        }

        return name.any { it !in VALID_CHARS }
    }

    @Suppress("all")
    private fun meetsCustomConditions(player: PlayerEntity): Boolean {
        val playerId = player.id
        return when {
            noGameMode && network.getPlayerListEntry(player.uuid)?.gameMode == null -> true
            InvalidGround.enabled && hasInvalidGround(player) -> true
            fakeEntityID && (playerId < 0 || playerId >= 1E+9) -> true
            duplicate && isADuplicate(player.gameProfile) -> true
            illegalName && hasIllegalName(player) -> true
            illegalPitch && abs(player.pitch) > 90 -> true
            AlwaysInRadius.enabled && !notAlwaysInRadiusSet.contains(playerId) -> true
            Age.enabled && ageSet.contains(playerId) -> true
            Armor.enabled && armorSet.contains(playerId) -> true
            needHit && !hitListSet.contains(playerId) -> true
            health && player.health > 20f -> true
            swung && !swungSet.contains(playerId) -> true
            critted && !crittedSet.contains(playerId) -> true
            attributes && !attributesSet.contains(playerId) -> true
            else -> false
        }
    }

    override fun isBot(entity: PlayerEntity): Boolean {
        return meetsCustomConditions(entity)
    }

    override fun reset() {
        flyingSet.clear()
        notAlwaysInRadiusSet.clear()
        hitListSet.clear()
        swungSet.clear()
        crittedSet.clear()
        attributesSet.clear()
        ageSet.clear()
        armorSet.clear()
    }
}
