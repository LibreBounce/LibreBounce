<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ModuleSetting, VectorSetting,} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let setting: ModuleSetting;
    const cSetting = setting as VectorSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    // todo: style input fields
    // todo: VECTOR_I and VECTOR_D should define if the input can take floating point numbers or not
</script>

<div class="setting">
    <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
    <input type="number" class="valueX" spellcheck="false"
           placeholder="X"
           bind:value={cSetting.value.x} on:input={handleChange}>
    <input type="number" class="valueY" spellcheck="false"
           placeholder="Y"
           bind:value={cSetting.value.y} on:input={handleChange}>
    <input type="number" class="valueZ" spellcheck="false"
           placeholder="Z"
           bind:value={cSetting.value.z} on:input={handleChange}>
</div>

<style lang="scss">
  @import "../../../colors.scss";

  .setting {
    padding: 7px 0px;
  }

  .name {
    font-weight: 500;
    color: $clickgui-text-color;
    font-size: 12px;
    margin-bottom: 5px;
  }

  .value {
    width: 100%;
    background-color: rgba($clickgui-base-color, .36);
    font-family: monospace;
    font-size: 12px;
    color: $clickgui-text-color;
    border: none;
    border-bottom: solid 2px $accent-color;
    padding: 5px;
    border-radius: 3px;
    transition: ease border-color .2s;

    &::-webkit-scrollbar {
      background-color: transparent;
    }
  }
</style>
