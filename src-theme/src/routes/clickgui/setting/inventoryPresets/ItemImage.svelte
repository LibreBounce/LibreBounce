<script lang="ts">
    import type {PresetItem} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";

    export let item: PresetItem;
</script>

{#if item.type === "CHOOSE"}
    <img src="{REST_BASE}/api/v1/client/resource/itemTexture?id={item.item}" alt={item.item}/>
{:else if item.type === "WEAPONS"}
    <img src="img/clickgui/icon-weapons.svg" alt="Weapons"/>
{:else if item.type === "TOOLS"}
    <img src="img/clickgui/icon-tools.svg" alt="Tools"/>
{:else if item.type === "FOOD"}
    <img src="img/clickgui/icon-food.svg" alt="Food"/>
{:else if item.type === "BLOCKS"}
    <img src="img/clickgui/icon-blocks.svg" alt="Blocks"/>
{:else if item.type === "ANY"}
    <img src="img/clickgui/icon-any.svg" alt="Any"/>
{/if}

<style lang="scss">
  img {
    width: 100%;
    height: 100%;
  }
</style>
