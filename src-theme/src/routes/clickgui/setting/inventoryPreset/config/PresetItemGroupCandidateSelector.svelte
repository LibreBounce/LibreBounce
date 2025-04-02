<script lang="ts">
    import type {PresetItem} from "../../../../../integration/types";
    import ItemGroupSelector from "./ItemGroupSelector.svelte";
    import {createEventDispatcher} from "svelte";

    export let items: PresetItem[];
    export let parentExpanded: boolean

    const dispatch = createEventDispatcher();

    function handleChange() {
        dispatch("change")
    }
</script>

<div class="wrapper">
    <div class="select-title">
        <span>Candidates</span>
    </div>
    <div class="candidates">
        <ItemGroupSelector
                bind:parentExpanded
                bind:items={items}
                on:change={handleChange}
                style="width: 100%px; max-height: 100%"
        />
    </div>
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;
  @use "select" as *;
  @use "item" as *;

  .candidates {
    margin-top: 20px;
    height: calc(100% - 40px);
  }

  .wrapper {
    position: relative;
    height: 100%;
  }
</style>
