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

<div class="container">
    {#each stacks as stack (stack)}
        <ItemStackView {stack}/>
    {/each}
</div>

<style lang="scss">
  @import "../../../../colors";

  .container {
    background-color: rgba($hotbar-base-color, 0.5);
    grid-template-columns: repeat(2, 1fr);
    padding: 4px;
    border-radius: 5px;
    display: grid;
    gap: 0.5rem;
  }
</style>
