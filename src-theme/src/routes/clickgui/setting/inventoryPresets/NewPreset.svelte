<script lang="ts">
    import {spaceSeperatedNames} from "../../../../theme/theme_config";
    import {createEventDispatcher} from "svelte";
    import type {AnyPresetItem, InventoryPreset} from "../../../../integration/types";

    export let value: InventoryPreset[];

    const dispatch = createEventDispatcher();

    function addNewPreset() {
        value = [...value, {
            items: Array.from({ length: 10 }, (): AnyPresetItem => ({ type: "ANY" }))
        }]

        dispatch("change")
    }
</script>

<button on:click|preventDefault={addNewPreset}>
    <span>{$spaceSeperatedNames ? "New Preset" : "NewPreset"}</span>
</button>

<style lang="scss">
  @use "sass:color";
  @use "../../../../colors.scss" as *;

  button {
    background-color: $accent-color;
    border-radius: 3px;
    border: none;
    width: 100%;
    height: 25px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 5px;
    cursor: pointer;

    &:hover {
      background-color: color.adjust(color.adjust($accent-color, $saturation: -30%), $lightness: -10%);
    }
  }

  span {
    color: $clickgui-text-color;
    font-weight: 500;
    font-size: 12px;
  }
</style>
