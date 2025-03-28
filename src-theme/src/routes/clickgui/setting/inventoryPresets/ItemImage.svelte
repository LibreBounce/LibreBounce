<script lang="ts">
    import type {PresetItem} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";

    export let item: PresetItem;
</script>

{#if item.type === "CHOOSE"}
    <img src="{REST_BASE}/api/v1/client/resource/itemTexture?id={item.item}" alt={item.item}/>
{:else if item.type === "SWORD"}
    <img src="img/items/sword.svg" alt="sword">
{/if}

<style lang="scss">
  img {
    width: 100%;
    height: 100%;
  }
</style>
