<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {listen} from "../../../../integration/ws";
    import type {PlayerInventoryEvent} from "../../../../integration/events";
    import ItemStackView from "./ItemStackView.svelte";

    let stacks: ItemStack[] = [];

    listen("playerInventory", (data: PlayerInventoryEvent) => {
        stacks = data.crafting;
    });
</script>

<div class="container" style="grid-template-columns: repeat({Math.floor(Math.sqrt(stacks.length))}, minmax(0, 1fr))">
    {#each stacks as stack (stack)}
        <ItemStackView {stack}/>
    {/each}
</div>

<style lang="scss">
  @import "../../../../colors";

  .container {
    background-color: rgba($hotbar-base-color, 0.34);
    padding: 4px;
    border-radius: 4px;
    display: grid;
    gap: 0.5rem;
  }
</style>
