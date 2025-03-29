@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.*
import net.ccbluex.liquidbounce.features.inventoryPresets.items.*
import net.minecraft.item.Item
import java.lang.reflect.Type

object PresetItemAdapter : JsonSerializer<PresetItem>, JsonDeserializer<PresetItem> {
    override fun serialize(src: PresetItem, typeOfSrc: Type?, context: JsonSerializationContext) = JsonObject().apply {
        add("type", context.serialize(src.type))

        if (src is ChoosePresetItem) {
            add("item", context.serialize(src.item))
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): PresetItem {
        val obj = json.asJsonObject

        return when (ItemType.findOrThrow(obj["type"].asString)) {
            ItemType.ANY -> AnyPresetItem
            ItemType.NONE -> NonePresetItem
            ItemType.TOOLS -> ToolsPresetItem
            ItemType.FOOD -> FoodPresetItem
            ItemType.BLOCKS -> BlocksPresetItem
            ItemType.WEAPONS -> WeaponsPresetItem
            ItemType.CHOOSE -> ChoosePresetItem(context.deserialize(obj["item"], Item::class.java))
        }
    }
}
