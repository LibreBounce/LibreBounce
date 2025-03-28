package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
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

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        if (!inGame) {
            return@handler
        }

        @Suppress("UNCHECKED_CAST")
        entities = world.entities.filter { entity ->
            entity is LivingEntity && entity.shouldBeShown()
        } as Iterable<LivingEntity>
    }

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEvent> {
        entities = emptyList()
    }

    override fun iterator(): Iterator<LivingEntity> = entities.iterator()
}
