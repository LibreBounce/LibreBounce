<script lang="ts">
    import {portal} from "../../../../../util/utils"
    import type {InventoryPreset} from "../../../../../integration/types";
    import {createEventDispatcher} from "svelte";
    import {fly, scale} from "svelte/transition"
    import {backOut} from "svelte/easing";
    import PresetItems from "./PresetItems.svelte";
    import ThrowItemsContainer from "./ThrowsItemContainer.svelte";
    import {scaleFactor} from "../../../clickgui_store";
    import PresetDeleteButton from "./PresetDeleteButton.svelte";

    export let preset: InventoryPreset
    export let idx: number

    const dispatch = createEventDispatcher();
    let throwsContainer: ThrowItemsContainer;
    let throwsRendered: number

    function handleClearAllThrows() {
        throwsContainer.clear();
    }

    function handleChange() {
        dispatch("change")
    }

    function handleClose() {
        throwsContainer.flush();
        dispatch("close")
    }

    function handleDelete() {
        dispatch("delete")
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="modal" use:portal on:click={handleClose} transition:fly={{duration: 200}}>
    <div class="container" on:click|stopPropagation transition:scale={{duration: 200, easing: backOut, start: 0.9}} style="transform: translateX(-50%) scale({$scaleFactor * 50}%)">
        <div class="title">
            <span>Inventory #{idx+1}</span>
            <div class="right">
                <PresetDeleteButton on:delete={handleDelete}>
                    Delete
                </PresetDeleteButton>
            </div>
        </div>

        <div class="items-container">
            <div class="header muted">HOTBAR</div>

            <PresetItems
                    bind:items={preset.items}
                    on:change={handleChange}
            />
        </div>

        <div class="throws-items-container">
            <div class="header">
                {throwsRendered}
                <span class="muted">THROWS</span>
                {#if throwsRendered > 0}
                    <span class="clear-throws" on:click={handleClearAllThrows}>Clear all</span>
                {/if}
            </div>

            <ThrowItemsContainer
                    bind:this={throwsContainer}
                    bind:throws={preset.throws}
                    bind:renderedLength={throwsRendered}
                    on:change={handleChange}
            />
        </div>
    </div>
</div>

<style lang="scss">
  @use "../../../../../colors.scss" as *;

  .container {
    transform-origin: top;
    position: fixed;
    border-radius: 3px;
    width: 700px;
    min-width: 700px;
    box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);
    background-color: rgba($clickgui-base-color, 0.9);
    left: 50%;
    top: 20px;
  }

  .modal {
    z-index: 9999;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba($clickgui-base-color, 0.3);
    backdrop-filter: blur(5px);
    color: $clickgui-text-color;
  }

  .right {
    margin-left: auto;
  }

  .items-container {
    padding: 20px;
  }

  .throws-items-container {
    padding: 0 20px 20px;
  }
  
  .clear-throws {
    cursor: pointer;
    text-decoration: underline dotted;
    font-size: 12px;
  }

  .header {
    margin-bottom: 10px;
    font-size: 16px;
    font-weight: 600;
    text-transform: uppercase;
    color: $clickgui-text-color;
  }

  .muted {
    color: rgba($clickgui-text-dimmed-color, 0.6);
  }

  .title {
    padding: 0 20px;
    height: 60px;
    position: relative;
    border-bottom: $accent-color solid 1px;
    background-color: rgba($clickgui-base-color, 0.5);
    color: $menu-text-color;
    border-radius: 3px 3px 0 0;

    display: flex;
    align-items: center;

    & > span {
      font-weight: 500;
      letter-spacing: 1px;
      font-size: 16px;
    }
  }
</style>
