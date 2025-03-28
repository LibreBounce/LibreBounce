<script lang="ts">
    import {portal} from "../../../../../util/portal_utils"
    import type {InventoryPreset} from "../../../../../integration/types";
    import {createEventDispatcher} from "svelte";

    export let preset: InventoryPreset

    const dispatch = createEventDispatcher();

    function handleClose() {
        dispatch("close")
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="modal" use:portal>

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
  }
</style>
