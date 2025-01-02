<script lang="ts">
    import type {Component as TComponent} from "../../../../../integration/types";
    import Component from "./Component.svelte";

    interface Props {
        components: TComponent[]
    }

    let {components}: Props = $props();
</script>

<div class="component-list">
    <div class="active-components">
        {#each components as c}
            <Component name={c.name} id={c.id}/>
        {/each}
    </div>
</div>

<style lang="scss">
    .component-list {
      padding: 15px;
    }

    .active-components {
      display: flex;
      flex-direction: column;
      row-gap: 10px;
    }
</style>