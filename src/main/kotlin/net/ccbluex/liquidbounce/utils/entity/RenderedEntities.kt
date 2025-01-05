package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.entity.LivingEntity
import java.util.*

object RenderedEntities : Iterable<LivingEntity>, EventListener, MinecraftShortcuts {
    private val registry = IdentityHashMap<EventListener, Unit>()

    private var entities: Iterable<LivingEntity> = emptyList()

    override val running: Boolean
        get() = registry.isNotEmpty()

    fun subscribe(subscriber: EventListener) {
        registry[subscriber] = Unit
    }

    fun unsubscribe(subscriber: EventListener) {
        registry.remove(subscriber)
    }

    private val tickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        @Suppress("UNCHECKED_CAST")
        entities = world.entities.filter {
            it is LivingEntity && it.shouldBeShown()
        } as Iterable<LivingEntity>
    }

    override fun iterator(): Iterator<LivingEntity> = entities.iterator()
}
