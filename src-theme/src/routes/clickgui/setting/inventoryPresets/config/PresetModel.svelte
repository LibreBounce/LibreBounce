<script lang="ts">
    import {portal} from "../../../../../util/portal_utils"
    import type {InventoryPreset} from "../../../../../integration/types";
    import {createEventDispatcher} from "svelte";
    import {fly, scale} from "svelte/transition"
    import {backOut} from "svelte/easing";
    import PresetItems from "./PresetItems.svelte";

    export let preset: InventoryPreset
    export let idx: number

    const dispatch = createEventDispatcher();

    function handleChange() {
        dispatch("change")
    }

    function handleClose() {
        dispatch("close")
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="modal" use:portal on:click={handleClose} transition:fly={{duration: 200}}>
    <div class="container" on:click|stopPropagation transition:scale={{duration: 200, easing: backOut, start: 0.9}}>
        <div class="title">
            <span>Inventory #{idx+1}</span>
            <button on:click={() => dispatch("delete")}>Delete</button>
        </div>
        <div class="items-container">
            <PresetItems
                    bind:items={preset.items}
                    on:change={handleChange}
            />
        </div>
    </div>
</div>

<style lang="scss">
  @use "../../../../../colors.scss" as *;

  .modal {
    z-index: 9999;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba($clickgui-base-color, 0.3);
    backdrop-filter: blur(5px);
    display: flex;
    align-items: center;
    justify-content: center;
    color: $clickgui-text-color;
  }

  .items-container {
    padding: 20px;
  }

  .title {
    padding: 0 20px;
    height: 60px;
    position: relative;
    border-bottom: $accent-color solid 1px;
    background-color: rgba($clickgui-base-color, 0.5);
    color: $menu-text-color;

    display: flex;
    align-items: center;

    & > span {
      font-weight: 500;
      letter-spacing: 1px;
      font-size: 16px;
    }

    & > button {
      cursor: pointer;
      margin-left: auto;
      border: none;
      padding: 5px 15px;
      background: $menu-error-color;
      color: $menu-text-color;
      border-radius: 3px;

      &:hover {
        background: rgba($menu-error-color, 0.9);
      }
    }
  }

  .container {
    border-radius: 3px;
    width: 700px;
    min-width: 700px;
    height: 300px;
    box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);
    background-color: rgba($clickgui-base-color, 0.9);
    overflow: hidden;
  }
</style>
