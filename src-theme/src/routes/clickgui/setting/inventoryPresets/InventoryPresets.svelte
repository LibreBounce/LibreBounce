<script lang="ts">
    import type {InventoryPresets, ModuleSetting} from "../../../../integration/types";
    import {spaceSeperatedNames} from "../../../../theme/theme_config";
    import InventoryPreset from "./InventoryPreset.svelte";
    import NewPreset from "./NewPreset.svelte";
    import {createEventDispatcher} from "svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as InventoryPresets;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting">
    <div class="head">
        <span class="title">{$spaceSeperatedNames ? "Inventory Presets" : "InventoryPresets"}</span>
    </div>

    <div class="presets" class:margin={cSetting.value.length > 0}>
        {#each cSetting.value as preset, idx}
            <InventoryPreset bind:preset idx={idx} on:change={handleChange} />
        {/each}
    </div>

    <NewPreset
            on:change={handleChange}
            bind:value={cSetting.value}
    />
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
    color: $clickgui-text-color;
  }

  .head {
    margin-bottom: 5px;
  }

  .presets {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 5px;
  }

  .margin {
    margin-bottom: 5px;
  }

  .title {
    color: $clickgui-text-color;
    font-size: 12px;
    font-weight: 600;
  }
</style>
