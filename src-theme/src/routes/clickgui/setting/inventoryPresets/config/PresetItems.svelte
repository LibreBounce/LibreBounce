<script lang="ts">
    import type {PresetItem} from "../../../../../integration/types";
    import {createEventDispatcher} from "svelte";
    import PresetItemComponent from "./PresetItemComponent.svelte";

    export let items: PresetItem[]
    let draggedIndex: number | null = null;

    const dispatch = createEventDispatcher();

    function handleChange() {
        items = [...items]
        dispatch("change")
    }

    function handleDragStart(event: DragEvent, index: number) {
        draggedIndex = index;
        event.dataTransfer?.setData('text/plain', '');
    }

    function handleDragOver(event: DragEvent, index: number) {
        event.preventDefault();
        if (draggedIndex === null || draggedIndex === index) return;

        [items[draggedIndex], items[index]] = [items[index], items[draggedIndex]];

        draggedIndex = index;
        dispatch("change");
    }

    function handleDragEnd() {
        draggedIndex = null;
        console.log("dragend")
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="items" on:mouseup|capture={handleDragEnd}>
    {#each items as item, idx (idx)}
        <div
                class="draggable"
                draggable="true"
                class:dragging={idx === draggedIndex}
                on:dragstart={(e) => handleDragStart(e, idx)}
                on:dragover={(e) => handleDragOver(e, idx)}
                on:dragend={handleDragEnd}
        >
            <PresetItemComponent bind:item idx={idx} on:change={handleChange} />
        </div>

        {#if idx === 0}
            <div class="divider"></div>
        {/if}
    {/each}
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;

  .draggable {
    cursor: grab;
    user-select: none;
  }

  .dragging {
    opacity: 0.5;
    cursor: grabbing;
  }

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
