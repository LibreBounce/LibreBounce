<script lang="ts">
    import type {ConfigurableSetting} from "../../../integration/types";
    import {onMount} from "svelte";
    import {
        getComponentSettings,
        setComponentSettings
    } from "../../../integration/rest";
    import GenericSetting from "../../clickgui/setting/common/GenericSetting.svelte";

    export let name: string;
    export let id: string;
    export let bottom: boolean;
    export let horizontalOffset: number;

    let element: HTMLElement | undefined;
    let configurable: ConfigurableSetting | undefined;

    let marginLeft = 0;

    $: horizontalOffset, (() => {
        if (!element) return;

        const bounding = element.getBoundingClientRect();

        if (bounding.left + bounding.width > window.innerWidth) {
            marginLeft = -(bounding.left + bounding.width - window.innerWidth);
        } else if (bounding.left < 0) {
            marginLeft = -bounding.left;
        } else {
            marginLeft = 0;
        }
    })();

    async function handleSettingChange() {
        await setComponentSettings(id, configurable!!);
        configurable = await getComponentSettings(id);
    }

    onMount(async () => {
        configurable = await getComponentSettings(id);
    });
</script>

<div class="settings-wrapper" class:bottom={bottom} bind:this={element}>
    <div class="settings" style="transform: translateX({marginLeft}px)">
        {#if configurable !== undefined}
            <GenericSetting path={name} bind:setting={configurable} on:change={handleSettingChange}/>
        {/if}
    </div>
</div>

<style lang="scss">
  @import "../../../colors";

  .settings-wrapper {
    position: absolute;
    top: -15px;
    left: 50%;
    transform: translateY(-100%) translateX(-50%);

    .settings {
      background-color: $hud-editor-descriptor-background-color;
      padding: 5px 10px;
      border-radius: 5px;
      width: 200px;
      box-shadow: $hud-editor-descriptor-shadow;
    }

    &::after {
      content: "";
      display: block;
      position: absolute;
      width: 0;
      height: 0;
      border-top: 8px solid transparent;
      border-bottom: 8px solid transparent;
      border-right: 8px solid $hud-editor-descriptor-background-color;
      left: 50%;
      bottom: -12px;
      transform: translateX(-50%) rotate(-90deg);
    }

    &.bottom {
      top: unset;
      bottom: -15px;
      transform: translateY(100%) translateX(-50%);;

      &::after {
        top: -12px;
        transform: translateX(-50%) rotate(90deg);
      }
    }
  }
</style>