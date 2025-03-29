<script lang="ts">
    import type {GenericPresetItem, PresetItem} from "../../../../../integration/types";
    import {spaceSeperatedNames} from "../../../../../theme/theme_config";
    import ItemImage from "../ItemImage.svelte";

    export let setItem: (item: PresetItem) => void

    const commonItems: PresetItem[] = [
        {
            type: "CHOOSE",
            item: "minecraft:diamond_sword"
        },
        {
            type: "CHOOSE",
            item: "minecraft:diamond_pickaxe"
        },
        {
            type: "CHOOSE",
            item: "minecraft:shield"
        },
        {
            type: "CHOOSE",
            item: "minecraft:cooked_beef"
        },
        {
            type: "CHOOSE",
            item: "minecraft:snowball"
        },
        {
            type: "CHOOSE",
            item: "minecraft:egg"
        },
        {
            type: "CHOOSE",
            item: "minecraft:fishing_rod"
        },
        {
            type: "CHOOSE",
            item: "minecraft:golden_apple"
        },
        {
            type: "CHOOSE",
            item: "minecraft:water_bucket"
        },
        {
            type: "BLOCKS"
        },
    ];

    interface GenericPresetItemList {
        item: GenericPresetItem
        name: string
    }

    const genericItems: (GenericPresetItemList)[] = [
        {
            item: { type: "WEAPONS" },
            name: "Weapons"
        },
        {
            item: { type: "TOOLS" },
            name: "Tools"
        },
        {
            item: { type: "ANY" },
            name: "AnyItem"
        }
    ]
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="selector">
    <div class="title">
        <span>{$spaceSeperatedNames ? "All Items" : "AllItems"}</span>
    </div>

    <div>
        <span class="items-group-title">{$spaceSeperatedNames ? "Common Items" : "CommonItems"}</span>
        <div class="common-wrapper">
            {#each commonItems as commonItem}
                <div class="common-item-wrapper" on:click={() => setItem(commonItem)}>
                    <div class="common-item">
                        <ItemImage bind:item={commonItem} />
                    </div>
                </div>
            {/each}
        </div>
    </div>

    <div>
        <span class="items-group-title">{$spaceSeperatedNames ? "Generic Items" : "GenericItems"}</span>
    </div>
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;

  .title {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 500;
    font-size: 16px;
  }

  .selector {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .items-group-title {
    font-size: 12px;
    color: rgba($clickgui-text-dimmed-color, 0.6);
    font-weight: 600;
    margin-left: 5px;
    text-transform: uppercase;
  }

  .common-wrapper {
    margin-top: 5px;
    display: flex;
    gap: 8px;
    justify-content: space-between;
    flex-wrap: wrap;
  }

  .common-item-wrapper {
    border-radius: 3px;
    background-color: color.adjust($clickgui-text-color, $lightness: -90%);
    width: 25px;
    height: 25px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
  }

  .common-item {
    width: 20px;
    height: 20px;
  }
</style>
