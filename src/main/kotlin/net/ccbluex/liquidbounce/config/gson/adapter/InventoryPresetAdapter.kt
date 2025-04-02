package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.features.inventoryPreset.InventoryPreset
import net.ccbluex.liquidbounce.features.inventoryPreset.items.PresetItemGroup
import net.ccbluex.liquidbounce.features.inventoryPreset.throwing.MaxStackGroup
import java.lang.reflect.Type

object InventoryPresetAdapter : JsonSerializer<InventoryPreset>, JsonDeserializer<InventoryPreset> {
    override fun serialize(
        src: InventoryPreset,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement = JsonObject().apply {
        add("items", context.serialize(src.items.map { it.second }))
        add("maxStacks", context.serialize(src.maxStacks))
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): InventoryPreset = with (json.asJsonObject) {
        val items = context.decode<Array<PresetItemGroup>>(get("items"))
        val throws = context.decode<Array<MaxStackGroup>>(get("maxStacks"))

        return InventoryPreset(items, throws)
    }

    private inline fun <reified T> JsonDeserializationContext.decode(element: JsonElement): T {
        return deserialize(element, object: TypeToken<T>() {}.type)
    }
}
