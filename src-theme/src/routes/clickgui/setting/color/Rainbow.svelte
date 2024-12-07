<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import type {ColorSetting, ModuleSetting} from "../../../../integration/types.js";
    import {spaceSeperatedNames} from "../../../../theme/theme_config";
    import Dropdown from "./../common/Dropdown.svelte";
    import "nouislider/dist/nouislider.css";
    import "./../nouislider.scss";
    import noUiSlider, {type API} from "nouislider";
    import ValueInput from "./../common/ValueInput.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as ColorSetting;

    const dispatch = createEventDispatcher();

    let slider: HTMLElement;
    let apiSlider: API;

    $: if (slider && !apiSlider) {
        apiSlider = noUiSlider.create(slider, {
            start: cSetting.rainbowSpeed,
            connect: "lower",
            range: {
                min: 1,
                max: 100,
            },
            step: 1,
        });

        apiSlider.on("update", (values) => {
            cSetting.rainbowSpeed = parseInt((values[0]).toString());
            setting = {...cSetting};
        });

        apiSlider.on("set", () => {
            dispatch("change");
        });
    }

    function handleRainbowModeChange() {
        setting = {...cSetting};
        dispatch("change");
    }
</script>

<div class="name">{$spaceSeperatedNames ? "Rainbow Mode" : "RainbowMode"}</div>
<Dropdown name={null} options={["None", "Cycle", "Pulse"]} bind:value={cSetting.rainbowMode}
          on:change={handleRainbowModeChange}/>
{#if cSetting.rainbowMode !== "None" }
    <div class="speed">
        <div class="name-speed">{"Speed"}</div>
        <div class="speed-value">
            <ValueInput valueType="int" value={cSetting.rainbowSpeed}
                        on:change={(e) => apiSlider.set(e.detail.value)}/>
        </div>
        <div bind:this={slider} class="speed-slider"></div>
    </div>
{/if}

<style lang="scss">
  @import "../../../../colors.scss";

  .speed {
    padding: 7px 0 2px 0;
    display: grid;
    grid-template-areas:
            "a b"
            "d d";
    grid-template-columns: 1fr max-content;
    column-gap: 5px;

    /* animation fix */
    min-height: 46px;
  }

  .speed-value {
    color: $clickgui-text-color;
    font-weight: 500;
    font-size: 12px;
  }

  .name-speed {
    font-weight: 500;
    color: $clickgui-text-color;
    font-size: 12px;
    margin-bottom: 5px;
  }

  .name {
    grid-area: a;
    font-weight: 500;
    color: $clickgui-text-color;
    font-size: 12px;
    margin-bottom: 5px;
    margin-top: 5px;
  }

  .speed-value {
    grid-area: b;
  }

  .speed-slider {
    grid-area: d;
    padding-right: 10px;
  }
</style>
