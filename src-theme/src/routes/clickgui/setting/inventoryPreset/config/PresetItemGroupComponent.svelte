<script lang="ts">
    import type {PresetItemGroup} from "../../../../../integration/types";
    import {clickOutside} from "../../../../../util/utils";
    import {scale} from "svelte/transition";
    import {createEventDispatcher} from "svelte";
    import PresetItemGroupPreview from "./PresetItemGroupPreview.svelte";
    import PresetItemGroupCandidateSelector from "./PresetItemGroupCandidateSelector.svelte";

    const dispatch = createEventDispatcher();

    export let group: PresetItemGroup;
    export let idx: number;

    let expanded = false;

    function clearItems() {
        group.items = [];
        expanded = false;

        dispatch("change");
    }

    function handleChange() {
        dispatch("change")
    }

    function handleClickOutside(event: MouseEvent) {
        const target = event.target as HTMLElement
        if (!target.closest(".group-candidate-selector")) {
            expanded = false
        }
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="wrapper">
    <div class="item-container item-background"
         class:active={expanded}
         class:hided={!expanded}
         on:click|preventDefault={() => expanded = !expanded}
    >
        <div class="image-wrapper">
            <PresetItemGroupPreview bind:group />
        </div>

        {#if group.items.length > 0}
            <button class="delete" on:click|stopPropagation={clearItems}>
                <img src="img/menu/icon-exit-danger.svg" alt="exit">
            </button>
        {/if}
    </div>

    {#if expanded}
        <div
                class="selector-container-wrapper selector-container"
                transition:scale={{duration: 200, start: 0.9}}
                on:click|preventDefault
                use:clickOutside={handleClickOutside}
        >
            <PresetItemGroupCandidateSelector
                    bind:parentExpanded={expanded}
                    bind:items={group.items}
                    on:change={handleChange}
            />

            <div class="slot">
                <span>{idx === 0 ? "Offhand" : idx}</span>
            </div>
        </div>
    {/if}
</div>


<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;
  @use "select" as *;
  @use "item" as *;

  .wrapper {
    position: relative;
  }

  .item-container {
    position: relative;
    width: 48px;
    height: 48px;
    border-radius: 6px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: 0.3s all ease;

    &:hover {
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

    & > .image-wrapper {
      filter: opacity(0.5);
    }
  }

  .selector-container {
    left: 50%;
    top: 50%;
    cursor: auto;
    transform-origin: left top;
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
