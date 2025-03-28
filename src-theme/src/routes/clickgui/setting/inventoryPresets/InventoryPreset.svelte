<script lang="ts">
    import type {InventoryPreset} from "../../../../integration/types";
    import ItemImage from "./ItemImage.svelte";
    import PresetModel from "./config/PresetModel.svelte";
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
    {#each preset.items as item}
        <div class="preset-item">
            <ItemImage bind:item />
        </div>
    {/each}
</div>

{#if configuring}
    <PresetModel
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
    background-color: color.adjust($clickgui-text-color, $lightness: -90%);
    border-radius: 3px;
    display: flex;
    justify-content: center;
    align-content: center;
    align-items: center;
  }
</style>
