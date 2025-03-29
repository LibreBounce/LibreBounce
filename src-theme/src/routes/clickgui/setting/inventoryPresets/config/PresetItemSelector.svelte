<script lang="ts">
    import type {PresetItem} from "../../../../../integration/types";
    import ItemImage from "../ItemImage.svelte";
    import {clickOutside} from "../../../../../util/utils";

    export let item: PresetItem;

    let expanded = false;
    let itemElement: HTMLElement;

    function clickHide(e: MouseEvent) {
        if (!itemElement.contains(e.target as Node)) {
            expanded = false;
        }
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="item"
     on:click|preventDefault={() => expanded = !expanded}
     use:clickOutside={() => expanded = false}
     bind:this={itemElement}
>
    <div class="image-wrapper">
        <ItemImage bind:item />
    </div>

    {#if expanded}
        <div class="selector">
            <h1>asdl</h1>
        </div>
    {/if}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;

  .item {
    width: 48px;
    height: 48px;
    background-color: rgba($clickgui-base-color, 0.85);
    outline: 1px solid color.adjust($clickgui-text-color, $lightness: -85%);
    position: relative;
    border-radius: 6px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: 0.3s all ease;

    &:hover {
      outline: 1px solid color.adjust($clickgui-text-color, $lightness: -70%);
    }
  }

  .selector {

  }

  .image-wrapper {
    width: 32px;
    height: 32px;
  }
</style>
