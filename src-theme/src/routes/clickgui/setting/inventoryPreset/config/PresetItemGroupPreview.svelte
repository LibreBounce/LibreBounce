<script lang="ts">
    import type {PresetItemGroup} from "../../../../../integration/types";
    import ItemImage from "../ItemImage.svelte";

    export let group: PresetItemGroup
</script>

<div class="preview" class:more={Math.max(1, group.items.length) !== 1}>
    {#if group.items.length === 0}
        <div class="item-container">
            <ItemImage item={{type: "NONE"}}/>
        </div>
    {:else}
        {#each group.items.slice(0, 3) as item}
            <div class="item-container">
                <ItemImage bind:item />
            </div>
        {/each}

        {#if group.items.length > 3}
            <div class="item-container counter">+{Math.min(9, group.items.length - 3)}</div>
        {/if}
    {/if}
</div>

<style lang="scss">
  .preview {
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
  }

  .more > .item-container {
    flex: 0 0 calc(50%);
  }

  .counter {
    flex: 0 0 50%;
    font-size: 10px;
    font-weight: 600;
  }

  .item-container {
    display: flex;
    align-items: center;
    justify-content: center;
  }
</style>
