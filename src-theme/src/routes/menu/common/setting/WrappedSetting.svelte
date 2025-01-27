<script lang="ts">
    import type {
        BooleanSetting as TBooleanSetting, ConfigurableSetting,
        ModuleSetting,
    } from "../../../../integration/types";
    import {fade, slide} from "svelte/transition";
    import SwitchSetting from "./SwitchSetting.svelte";
    import GenericSetting from "../../../clickgui/setting/common/GenericSetting.svelte";
    import {quintOut} from "svelte/easing";
    import {convertToSpacedString} from "../../../../theme/theme_config";

    interface Props {
        setting: ModuleSetting
    }

    interface NesterSetting {
        name: string;
        valueType: string;
        value: ModuleSetting[];
    }

    const {setting}: Props = $props();
    const nester = setting as NesterSetting;

    const enabledSetting = nester.value[0] as TBooleanSetting;
    let nestedSettings: ModuleSetting[] = [];
    if (nester.valueType === "TOGGLEABLE") {
        nestedSettings = nester.value.slice(1);
    } else if (nester.valueType === "CONFIGURABLE") {
        nestedSettings = nester.value;
    }

    let expanded = $state(false);
    let wrappedSettingElement: HTMLElement;
    let headerElement: HTMLElement;
    let rulerElement: HTMLElement | null = $state(null);

    function handleWrapperClick(e: MouseEvent) {
        if (!expanded) {
            expanded = true;
        } else {
            expanded = !headerElement.contains(e.target as Node);
        }
    }

    function handleWindowClick(e: MouseEvent) {
        if (!wrappedSettingElement) return;

        if (!wrappedSettingElement.contains(e.target as Node)) {
            expanded = false;
        }
    }
</script>

<svelte:window on:click={handleWindowClick}/>

<div class="wrapped-setting" class:expanded class:has-nested-settings={nestedSettings.length > 0}
     onclick={handleWrapperClick} bind:this={wrappedSettingElement}
style="min-width: {rulerElement?.getBoundingClientRect()?.width ?? 0}px">
    <div class="header" bind:this={headerElement}>
        {#if nester.valueType === "TOGGLEABLE"}
            <SwitchSetting title={convertToSpacedString(nester.name)} bind:value={enabledSetting.value}/>
        {:else if nester.valueType === "CONFIGURABLE"}
            <span class="configurable-title">{convertToSpacedString(nester.name)}</span>
        {:else }
            Unsupported value type {nester.valueType}
        {/if}
        {#if nestedSettings.length > 0}
            <img src="img/menu/icon-select-arrow.svg" alt="expand">
        {/if}
    </div>

    {#if expanded && nestedSettings.length > 0}
        <div class="nested-settings" transition:fade|global={{ duration: 200, easing: quintOut }}>
            {#each nestedSettings as setting (setting.name)}
                <div transition:slide|global={{ duration: 200, easing: quintOut }}>
                    <GenericSetting skipAnimationDelay={true} path="menu" {setting}/>
                </div>
            {/each}
        </div>
    {/if}

    <div class="ruler" bind:this={rulerElement}>
        {#each nestedSettings as setting (setting.name)}
            <GenericSetting skipAnimationDelay={true} path="menu" {setting}/>
        {/each}
    </div>
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .configurable-title {
    color: $menu-text-color;
    font-size: 20px;
    font-weight: 500;
  }

  .wrapped-setting {
    position: relative;
    min-width: 250px;

    &.expanded {
      .header {
        border-radius: 5px 5px 0 0;
      }
    }

    &.has-nested-settings {
      cursor: pointer;

      .header {
        background-color: rgba($menu-base-color, .36);
        padding: 20px;
        display: flex;
        column-gap: 20px;
        align-items: center;
        justify-content: space-between;
        border-radius: 5px;
        transition: ease border-radius .2s;
      }
    }
  }

  .ruler,
  .nested-settings {
    position: absolute;
    z-index: 1000;
    border-radius: 0 0 5px 5px;
    background-color: rgba($menu-base-color, 0.9);
    padding: 10px 13px;
    zoom: 1.5;
  }

  .ruler {
    visibility: hidden;
  }
</style>

