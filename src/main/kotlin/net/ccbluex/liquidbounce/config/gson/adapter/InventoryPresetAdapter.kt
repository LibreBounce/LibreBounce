package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.features.inventoryPresets.InventoryPreset
import net.ccbluex.liquidbounce.features.inventoryPresets.items.PresetItem
import net.minecraft.item.Item
import java.lang.reflect.Type

object InventoryPresetAdapter : JsonSerializer<InventoryPreset>, JsonDeserializer<InventoryPreset> {
    override fun serialize(
        src: InventoryPreset,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement = JsonObject().apply {
        add("items", context.serialize(src.items.map { it.second }))
        add("throws", context.serialize(src.throws))
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): InventoryPreset = with (json.asJsonObject) {
        val items = context.decode<Array<PresetItem>>(get("items"))
        val throws = context.decode<Set<Item>>(get("throws"))

        return InventoryPreset(items, throws)
    }

    private inline fun <reified T> JsonDeserializationContext.decode(element: JsonElement): T {
        return deserialize(element, object: TypeToken<T>() {}.type)
    }
}
