<script lang="ts">
    import type {InventoryPreset} from "../../../../integration/types";
    import ItemImage from "./ItemImage.svelte";
    import PresetModel from "./config/PresetModel.svelte";
    import {createEventDispatcher} from "svelte";

    export let preset: InventoryPreset
    export let idx: number

    const dispatch = createEventDispatcher();

    function handleChange() {
        dispatch("change")
    }

    function handleDelete() {
        configuring = false
        dispatch("delete")
    }

    function handleCopy() {
        configuring = false
        dispatch("copy")
    }

    let configuring = false
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="preset" on:click={() => configuring = true}>
    {#each preset.items as item, idx (idx)}
        <div class="preset-item">
            {#if item.type !== "NONE"}
                <ItemImage bind:item />
            {/if}
        </div>
    {/each}
</div>

{#if configuring}
    <PresetModel
            bind:preset
            bind:idx
            on:close={() => configuring = false}
            on:change={handleChange}
            on:delete={handleDelete}
            on:copy={handleCopy}
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
