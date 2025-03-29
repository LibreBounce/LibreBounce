<script lang="ts">
    import type {PresetItem} from "../../../../../integration/types";
    import {spaceSeperatedNames} from "../../../../../theme/theme_config";

    export let setItem: (item: PresetItem) => void
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="selector" on:click={() => setItem({type: "NONE"})}>
    <div class="title">
        <span>{$spaceSeperatedNames ? "All Items" : "AllItems"}</span>
    </div>
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;

  .title {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .selector {
    display: flex;
  }
</style>
