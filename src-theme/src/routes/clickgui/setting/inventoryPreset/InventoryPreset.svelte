<script lang="ts">
    import type {InventoryPreset} from "../../../../integration/types";
    import ItemImage from "./ItemImage.svelte";
    import PresetModal from "./config/PresetModal.svelte";
    import {createEventDispatcher} from "svelte";

    export let preset: InventoryPreset

    const dispatch = createEventDispatcher();

    function handleChange() {
        dispatch("change")
    }

    let configuring = false
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="preset" on:click={() => configuring = true}>
    {#each preset.items as group, idx (idx)}
        <div class="preset-item">
            {#if group.items[0]}
                {@const firstItem = group.items[0]}
                {#if firstItem.type !== "NONE"}
                    <div class="img-wrapper">
                        <ItemImage item={firstItem} />
                    </div>
                {/if}
            {/if}
        </div>
    {/each}
</div>

{#if configuring}
    <PresetModal
            bind:preset
            on:close={() => configuring = false}
            on:change={handleChange}
    />
{/if}

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
    outline: 1px solid color.adjust($clickgui-text-color, $lightness: -90%);
    border-radius: 3px;
    display: flex;
    justify-content: center;
    align-content: center;
    align-items: center;
  }

  .img-wrapper {
    width: 16px;
    height: 16px;
  }
</style>
