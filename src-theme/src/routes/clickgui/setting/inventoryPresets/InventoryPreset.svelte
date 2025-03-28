<script lang="ts">
    import type {InventoryPreset} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";

    export let preset: InventoryPreset
</script>

<div class="preset">
    {#each preset.items as item}
        <div class="preset-item">
            {#if item !== "minecraft:air"}
                <img class="icon" src="{REST_BASE}/api/v1/client/resource/itemTexture?id={item}" alt={item}/>
            {/if}
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

    & > img {
      width: 100%;
      height: 100%;
    }
  }
</style>
