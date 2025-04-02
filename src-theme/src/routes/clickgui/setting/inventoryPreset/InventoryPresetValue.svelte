<script lang="ts">
    import type {InventoryPresetValue, ModuleSetting} from "../../../../integration/types";
    import {spaceSeperatedNames} from "../../../../theme/theme_config";
    import InventoryPreset from "./InventoryPreset.svelte";
    import {createEventDispatcher} from "svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as InventoryPresetValue;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="setting">
    <div class="head">
        <span class="title">{$spaceSeperatedNames ? "Inventory Preset" : "InventoryPreset"}</span>
    </div>

    <div class="presets">
        <InventoryPreset
                bind:preset={cSetting.value}
                on:change={handleChange}
        />
    </div>
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
    color: $clickgui-text-color;
  }

  .head {
    margin-bottom: 10px;
  }

  .presets {
    margin-bottom: 5px;
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 5px;
  }

  .title {
    color: $clickgui-text-color;
    font-size: 12px;
    font-weight: 600;
  }
</style>
