<script lang="ts">
    import type {
        BooleanSetting as TBooleanSetting,
        ModuleSetting,
        TogglableSetting
    } from "../../../../integration/types";
    import {fade, slide} from "svelte/transition";
    import SwitchSetting from "./SwitchSetting.svelte";
    import GenericSetting from "../../../clickgui/setting/common/GenericSetting.svelte";
    import {quintOut} from "svelte/easing";
    import {convertToSpacedString} from "../../../../theme/theme_config";

    interface Props {
        setting: ModuleSetting
    }

    const {setting}: Props = $props();
    const toggleable = setting as TogglableSetting;

    const enabledSetting = toggleable.value[0] as TBooleanSetting;
    let nestedSettings = toggleable.value.slice(1);

    let expanded = $state(false);
    let wrappedSettingElement: HTMLElement;
    let headerElement: HTMLElement;

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
     onclick={handleWrapperClick} bind:this={wrappedSettingElement}>
    <div class="header" bind:this={headerElement}>
        <div class="setting">
            <SwitchSetting title={convertToSpacedString(toggleable.name)} bind:value={enabledSetting.value}/>
        </div>
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
</div>

<style lang="scss">
  @use "../../../../colors.scss" as *;

  .wrapped-setting {
    position: relative;

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

  .nested-settings {
    position: absolute;
    z-index: 1000;
    width: 100%;
    border-radius: 0 0 5px 5px;
    max-height: 250px;
    background-color: rgba($menu-base-color, 0.9);
    padding: 15px 20px;
  }
</style>

