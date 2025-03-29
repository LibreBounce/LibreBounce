<script lang="ts">
    import type {GenericPresetItem, PresetItem} from "../../../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../../theme/theme_config";
    import ItemImage from "../ItemImage.svelte";
    import {onMount} from "svelte";
    import {getRegistries} from "../../../../../integration/rest";
    import VirtualList from "../../blocks/VirtualList.svelte";
    import {REST_BASE} from "../../../../../integration/host";

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
            item: "minecraft:ender_pearl"
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
            item: "minecraft:enchanted_golden_apple"
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

    const genericItems: GenericPresetItemList[] = [
        {
            item: { type: "WEAPONS" },
            name: "Weapons"
        },
        {
            item: { type: "TOOLS" },
            name: "Tools"
        },
        {
            item: { type: "FOOD" },
            name: "Food"
        },
        {
            item: { type: "BLOCKS" },
            name: "Blocks"
        },
        {
            item: { type: "ANY" },
            name: "AnyItem"
        }
    ]

    interface TItem {
        name: string;
        identifier: string;
    }

    let items: TItem[] = [];
    let renderedItems: TItem[] = items;
    let searchQuery = "";

    $: {
        let filteredItems = items;

        if (searchQuery) {
            filteredItems = filteredItems.filter(b => b.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }

        renderedItems = filteredItems;
    }

    onMount(async () => {
        let registries = (await getRegistries()).items;

        if (registries !== undefined) {
            items = registries.sort((a, b) => a.identifier.localeCompare(b.identifier));
        }
    });
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="selector">
    <div class="title">
        <span>{searchQuery === "" ? ($spaceSeperatedNames ? "All Items" : "AllItems") : "Search"}</span>
    </div>

    <div class="search-wrapper">
        <div class="search">
            <input type="text" placeholder="Search items..." class="search-input" bind:value={searchQuery} spellcheck="false">
            <div class="search-icon">
                <img src="img/menu/icon-pen.svg" alt="Search" />
            </div>
        </div>
    </div>

    {#if searchQuery === ""}
        <div>
            <span class="items-group-title">Common Items</span>
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
            <span class="items-group-title">Generic Items</span>
            <div class="generic-wrapper">
                {#each genericItems as genericItem}
                    <div class="generic-item" on:click={() => setItem(genericItem.item)}>
                        <div class="img-wrapper">
                            <div class="img">
                                <ItemImage bind:item={genericItem.item} />
                            </div>
                        </div>
                        <span>{$spaceSeperatedNames ? convertToSpacedString(genericItem.name) : genericItem.name}</span>
                    </div>
                {/each}
            </div>
        </div>
    {:else}
        {#if renderedItems.length > 0}
            <div class="results">
                <VirtualList items={renderedItems} let:item>
                    <div class="result-item" on:click={() => setItem({type: "CHOOSE", item: item.identifier})}>
                        <div class="icon-wrapper">
                            <img class="icon" src="{REST_BASE}/api/v1/client/resource/itemTexture?id={item.identifier}" alt={item.identifier}/>
                        </div>

                        <span class="name">
                            {item.name}
                        </span>
                    </div>
                </VirtualList>
            </div>
        {:else}
            <span class="items-group-title">No Results</span>
        {/if}
    {/if}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;

  .result-item {
    display: flex;
    height: 40px;
    align-items: center;
    gap: 10px;
    position: relative;
    cursor: pointer;

    &:hover {
      .name {
        color: $clickgui-text-color;
      }

      .icon-wrapper {
        background-color: color.adjust($clickgui-text-color, $lightness: -80%);
      }
    }
  }

  .icon-wrapper {
    transition: background-color 0.3s ease;
    background-color: color.adjust($clickgui-text-color, $lightness: -90%);
    border-radius: 3px;
    min-width: 30px;
    min-height: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .icon {
    width: 20px;
    height: 20px;
  }

  .name {
    transition: color 0.3s ease;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: $clickgui-text-dimmed-color;
  }

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
    height: 100%;
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
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
  }

  .common-item {
    width: 20px;
    height: 20px;
  }

  .generic-wrapper {
    margin-top: 5px;
    display: flex;
    flex-direction: column;
  }

  .generic-item {
    display: flex;
    width: 100%;
    height: 40px;
    align-items: center;
    gap: 10px;
    cursor: pointer;

    & > .img-wrapper {
      width: 25px;
      height: 25px;
    }

    & > span {
      color: $clickgui-text-dimmed-color;
      display: flex;
      font-size: 15px;
      transition: color 0.3s ease;
    }

    &:hover {

      & > span {
        color: $clickgui-text-color;
      }
    }
  }

  .search {
    position: relative;
  }

  .search-input {
    width: 100%;
    height: 35px;
    border-radius: 3px;
    border: none;
    background: transparent;
    padding-left: 35px;
    outline: solid 1px color.adjust($clickgui-text-color, $lightness: -90%);
    color: white;
  }

  .search-icon {
    position: absolute;
    left: 0;
    top: 0;
    width: 35px;
    height: 35px;
    display: flex;
    align-items: center;
    justify-content: center;

    & > img {
      width: 20px;
      height: 20px;
    }
  }

  .results {
    height: 100%;
    overflow-y: auto;
    overflow-x: hidden;
  }
</style>
