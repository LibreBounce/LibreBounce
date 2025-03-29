<script lang="ts">
    import type {PresetItem} from "../../../../../integration/types";
    import {createEventDispatcher} from "svelte";
    import PresetItemComponent from "./PresetItemComponent.svelte";

    export let items: PresetItem[]

    const dispatch = createEventDispatcher();

    function handleChange() {
        items = [...items]
        dispatch("change")
    }
</script>

<div class="items">
    {#each items as item, idx (idx)}
        <PresetItemComponent bind:item idx={idx} on:change={handleChange} />

        {#if idx === 0}
            <div class="divider"></div>
        {/if}
    {/each}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;

  .items {
    width: 100%;
    display: flex;
    justify-content: space-between;
    position: relative;
  }

  .divider {
    width: 2px;
    position: relative;
  }

  .divider::after {
    content: '';
    position: absolute;
    background-color: color.adjust($clickgui-text-color, $lightness: -85%);
    left: 0;
    top: 7px;
    bottom: 7px;
    right: 0;
  }
</style>
