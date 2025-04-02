<script lang="ts">
    import type {MaxStacksGroup} from "../../../../../../integration/types";
    import noUiSlider, {type API} from "nouislider";
    import ValueInput from "../../../common/ValueInput.svelte";
    import {createEventDispatcher, onMount} from "svelte";
    import ItemGroupSelector from "../ItemGroupSelector.svelte";

    export let group: MaxStacksGroup

    const dispatch = createEventDispatcher();

    let slider: HTMLElement;
    let apiSlider: API;

    onMount(() => {
        apiSlider = noUiSlider.create(slider, {
            start: group.stacks,
            connect: "lower",
            range: {
                min: 0,
                max: 2000,
            },
            step: 1,
        });

        apiSlider.on("update", (values) => {
            group.stacks = parseInt(values[0].toString());
        });

        apiSlider.on("set", () => {
            dispatch("change");
        });
    });

    function handleChange() {
        dispatch("change")
    }

    function handleDelete() {
        dispatch("delete")
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-no-noninteractive-element-interactions -->
<div class="container-wrapper">
    <div class="container">
        <ItemGroupSelector bind:items={group.items} on:change={handleChange} style="width: 309px; max-height: 70px;"/>

        <div class="slider-wrapper">
            <div class="slider-container">
                <div class="slider-top">
                    <span class="slider-left">Max</span>
                    <div class="slider-right">
                        <ValueInput valueType="int" value={group.stacks}
                                    on:change={(e) => apiSlider.set(e.detail.value)}/>
                        <span>
                            {#if group.stacks === 1}
                                item
                            {:else}
                                items
                            {/if}
                        </span>
                    </div>
                </div>
                <div bind:this={slider} class="slider"></div>
            </div>
        </div>
    </div>
    <div class="delete">
        <img src="img/menu/icon-exit-danger.svg" alt="exit" on:click={handleDelete}>
    </div>
</div>

<style lang="scss">
    @use "sass:color";
    @use "../../../../../../colors.scss" as *;
    @use "../select" as *;
    @use "../item" as *;

    .container-wrapper {
      display: flex;
    }

    .slider-top {
      display: flex;
      font-size: 12px;
    }

    .slider-right {
      margin-left: auto;
    }

    .slider-wrapper {
      flex-grow: 1;
      max-height: 35px;
      display: flex;
      flex-direction: column;
      justify-content: center;
    }

    .delete {
      flex-shrink: 0;
      height: 35px;
      width: 35px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;

      & > img {
        width: 16px;
        height: 16px;
        opacity: 0.5;
        transition: opacity 0.3s ease;

        &:hover {
          opacity: 1;
        }
      }
    }

    .item-group-selector {
      width: 307px;
      max-height: 70px;
    }

    .container {
      gap: 10px;
      display: flex;
      width: 100%;
    }

    .slider {
      margin-top: 3px;
      padding-right: 10px;
    }
</style>
