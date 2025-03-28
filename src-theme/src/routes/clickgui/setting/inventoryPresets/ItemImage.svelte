<script lang="ts">
    import type {PresetItem} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";

    export let item: PresetItem;
</script>

{#if item.type === "CHOOSE"}
    <img src="{REST_BASE}/api/v1/client/resource/itemTexture?id={item.item}" alt={item.item}/>
{:else if item.type === "SWORD"}
    <img class="white" src="{REST_BASE}/api/v1/client/resource/itemTexture?id=minecraft:diamond_sword" alt="Sword"/>
{:else if item.type === "AXE"}
    <img class="white" src="{REST_BASE}/api/v1/client/resource/itemTexture?id=minecraft:diamond_axe" alt="Axe"/>
{:else if item.type === "PICKAXE"}
    <img class="white" src="{REST_BASE}/api/v1/client/resource/itemTexture?id=minecraft:diamond_pickaxe" alt="Pickaxe"/>
{:else if item.type === "FOOD"}
    <img class="white" src="{REST_BASE}/api/v1/client/resource/itemTexture?id=minecraft:cooked_beef" alt="Food"/>
{:else if item.type === "BLOCK"}
    <img class="white" src="{REST_BASE}/api/v1/client/resource/itemTexture?id=minecraft:oak_log" alt="Block"/>
{:else if item.type === "POTION"}
    <img class="white" src="{REST_BASE}/api/v1/client/resource/itemTexture?id=minecraft:potion" alt="Potion"/>
{/if}

<style lang="scss">
  img {
    width: 100%;
    height: 100%;
  }

  .white {
    filter: brightness(0) invert(1);
  }
</style>
