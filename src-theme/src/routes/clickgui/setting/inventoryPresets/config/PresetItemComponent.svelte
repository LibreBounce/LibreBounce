<script lang="ts">
    import type {PresetItem} from "../../../../../integration/types";
    import ItemImage from "../ItemImage.svelte";
    import {clickOutside} from "../../../../../util/utils";
    import PresetItemSelector from "./PresetItemSelector.svelte";
    import {scale} from "svelte/transition";
    import {createEventDispatcher} from "svelte";

    const dispatch = createEventDispatcher();

    export let item: PresetItem;
    export let idx: number;

    let expanded = false;

    function setItem(newItem: PresetItem) {
        item = newItem;
        expanded = false;

        dispatch("change");
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="wrapper" use:clickOutside={() => expanded = false}>
    <div class="item"
         class:active={expanded}
         class:hided={!expanded}
         on:click|preventDefault={() => expanded = !expanded}
    >
        <div class="image-wrapper">
            {#if item.type !== "NONE"}
                <ItemImage bind:item />
            {/if}
        </div>

        {#if item.type !== "NONE"}
            <button class="delete" on:click|stopPropagation={() => setItem({type: "NONE"})}>
                <img src="img/menu/icon-exit-danger.svg" alt="exit">
            </button>
        {/if}
    </div>

    {#if expanded}
        <div class="selector-container" transition:scale={{duration: 200, start: 0.9}} on:click|preventDefault>
            <PresetItemSelector setItem={setItem} />

            <div class="slot">
                <span>{idx === 0 ? "Offhand" : idx}</span>
            </div>
        </div>
    {/if}
</div>


<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;

  .item {
    position: relative;
    width: 48px;
    height: 48px;
    background-color: rgba($clickgui-base-color, 0.85);
    outline: 1px solid color.adjust($clickgui-text-color, $lightness: -85%);
    border-radius: 6px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: 0.3s all ease;

    &:hover {
      outline: 1px solid color.adjust($clickgui-text-color, $lightness: -70%);

      &.hided .delete {
        opacity: 1;
        pointer-events: all;
      }
    }
  }

  .delete {
    background: none;
    border: none;
    position: absolute;
    left: 100%;
    top: 0;
    transform: translate(-50%, -50%);
    transition: opacity 0.3s ease;
    opacity: 0;
    cursor: pointer;
    width: 16px;
    height: 16px;
    pointer-events: none;

    & > img {
      width: 100%;
      height: 100%;
    }
  }

  .active {
    outline: 1px solid $accent-color !important;
  }

  .selector-container {
    z-index: 9999;
    position: absolute;
    width: 250px;
    height: 470px;
    padding: 20px;
    background-color: rgba($clickgui-base-color, 0.95);
    outline: 1px solid color.adjust($clickgui-text-color, $lightness: -85%);
    transform: translate(calc(-50% + 48px/2), 15px);
    border-radius: 3px;
  }

  .slot {
    position: absolute;
    font-size: 12px;
    font-weight: 500;
    left: 0;
    top: 0;
    padding: 0 5px;
    outline: 1px solid color.adjust($clickgui-text-color, $lightness: -85%);
    min-width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 3px 0;
  }

  .image-wrapper {
    width: 32px;
    height: 32px;
  }
</style>
