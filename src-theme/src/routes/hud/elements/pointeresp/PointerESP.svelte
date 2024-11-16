<script lang="ts">

    import {listen} from "../../../../integration/ws";
    import type {PointerInfoEvent} from "../../../../integration/events";
    import type {Pointer} from "../../../../integration/types";
    import PointerView from "./PointerView.svelte";

    let pointers: Pointer[] | undefined;

    listen("pointerInfo", (e: PointerInfoEvent) => {
        pointers = e.pointers;
    });
</script>

<div class="container">
    {#if pointers}
        {#each pointers as pointer}
            <PointerView {...pointer}/>
        {/each}
    {/if}
</div>

<style lang="scss">
  .container {
    position: relative;
  }
</style>
