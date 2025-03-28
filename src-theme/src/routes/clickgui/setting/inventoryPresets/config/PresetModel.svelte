<script lang="ts">
    import {portal} from "../../../../../util/portal_utils"
    import type {InventoryPreset} from "../../../../../integration/types";
    import {createEventDispatcher} from "svelte";
    import {fly} from "svelte/transition"

    export let preset: InventoryPreset
    export let idx: number

    const dispatch = createEventDispatcher();

    function handleClose() {
        dispatch("close")
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="modal" use:portal on:click={handleClose} transition:fly={{duration: 200}}>
    <div class="container" on:click|stopPropagation transition:fly={{duration: 200, y: -20}}>
        <span class="title">Inventory #{idx+1}</span>
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
    background-color: rgba($clickgui-base-color, 0.7);
    backdrop-filter: blur(10px);
    display: flex;
    align-items: center;
    justify-content: center;
    color: $clickgui-text-color;
  }

  .container {
    padding: 20px;
    border-radius: 3px;
    width: 600px;
    height: 300px;
    box-shadow: 0 0 10px rgba($clickgui-base-color, 0.5);
    background-color: rgba($clickgui-base-color, 0.9);
  }
</style>
