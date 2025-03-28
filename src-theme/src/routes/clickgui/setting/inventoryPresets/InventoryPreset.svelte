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
  @use "../../../../colors.scss" as *;

  .preset {
    display: flex;
    justify-content: space-between;
  }

  .preset-item {
    width: 20px;
    height: 20px;
    background-color: rgba($clickgui-base-color, 0.5);
    border-radius: 3px;
  }
</style>
