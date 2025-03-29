<script lang="ts">
    import type {PresetItem} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";

    export let item: PresetItem;
</script>

{#if item.type === "CHOOSE"}
    <img src="{REST_BASE}/api/v1/client/resource/itemTexture?id={item.item}" alt={item.item}/>
{:else if item.type === "WEAPONS"}
    <img class="white" src="{REST_BASE}/api/v1/client/resource/itemTexture?id=minecraft:diamond_sword" alt="Sword"/>
{:else if item.type === "TOOLS"}
    <img class="white" src="{REST_BASE}/api/v1/client/resource/itemTexture?id=minecraft:diamond_pickaxe" alt="Pickaxe"/>
{:else if item.type === "FOOD"}
    <img class="white" src="{REST_BASE}/api/v1/client/resource/itemTexture?id=minecraft:cooked_beef" alt="Food"/>
{:else if item.type === "BLOCKS"}
    <img class="white" src="{REST_BASE}/api/v1/client/resource/itemTexture?id=minecraft:oak_log" alt="Block"/>
{/if}

<style lang="scss">
  img {
    width: 100%;
    height: 100%;
  }

  .white {
    filter: grayscale(1);
  }
</style>
