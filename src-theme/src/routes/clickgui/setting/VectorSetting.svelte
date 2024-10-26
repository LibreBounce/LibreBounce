<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {BlockHitResult, ModuleSetting, VectorSetting,} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {getCrosshairData, getPlayerData} from "../../../integration/rest";

    export let setting: ModuleSetting;
    const cSetting = setting as VectorSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = {...cSetting};
        dispatch("change");
    }

    const isDouble = setting.valueType === "VECTOR_D";

    async function locate() {
        const hitResult = await getCrosshairData();

        console.log(JSON.stringify(hitResult));
        // Check if crosshair data is block and convert to BlockHitResult
        if (hitResult.type === "block") {
            const blockHitResult = hitResult as BlockHitResult;

            cSetting.value.x = blockHitResult.blockPos.x;
            cSetting.value.y = blockHitResult.blockPos.y;
            cSetting.value.z = blockHitResult.blockPos.z;
        } else {
            const playerData = await getPlayerData();
            cSetting.value.x = playerData.blockPosition.x;
            cSetting.value.y = playerData.blockPosition.y;
            cSetting.value.z = playerData.blockPosition.z;
        }
        handleChange();
    }
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
    {#if !isDouble}
        <button on:click={locate}>Locate</button>
    {/if}
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
