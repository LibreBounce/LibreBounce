<script lang="ts">
    import type {InventoryPreset} from "../../../../integration/types";
    import ItemImage from "./ItemImage.svelte";

    export let preset: InventoryPreset
</script>

<div class="preset">
    {#each preset.items as item}
        <div class="preset-item">
            <ItemImage bind:item />
        </div>
    {/each}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../colors.scss" as *;

  .preset {
    display: flex;
    justify-content: space-between;
    cursor: pointer;
  }

  .preset-item {
    width: 20px;
    height: 20px;
    background-color: color.adjust($clickgui-text-color, $lightness: -90%);
    border-radius: 3px;
    display: flex;
    justify-content: center;
    align-content: center;
    align-items: center;
  }
</style>
