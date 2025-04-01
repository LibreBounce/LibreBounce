<script lang="ts">
    import type {MaxStacksGroup, PresetItem} from "../../../../../../integration/types";
    import noUiSlider, {type API} from "nouislider";
    import ValueInput from "../../../common/ValueInput.svelte";
    import {createEventDispatcher, onDestroy, onMount} from "svelte";
    import {scaleFactor} from "../../../../clickgui_store";
    import {clickOutside, portal} from "../../../../../../util/utils";
    import {scale} from "svelte/transition";
    import PresetItemSelector from "../PresetItemSelector.svelte";
    import ItemImage from "../../ItemImage.svelte";

    export let group: MaxStacksGroup

    const dispatch = createEventDispatcher();

    let slider: HTMLElement;
    let apiSlider: API;

    let expanded: boolean = false

    let addRef: HTMLElement;
    let addElementPosition = { top: 0, left: 0, width: 0, height: 0 };
    const resizeObserver = new ResizeObserver(updatePosition);

    let choiceItems: string[];

    function updatePosition() {
        if (!addRef) return;
        const rect = addRef.getBoundingClientRect();
        addElementPosition = {
            top: rect.top + window.scrollY,
            left: rect.left + window.scrollX,
            width: rect.width,
            height: rect.height
        };
    }

    onMount(() => {
        window.addEventListener('resize', updatePosition);
        if (addRef) resizeObserver.observe(addRef);
        updatePosition();

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

    onDestroy(() => {
        window.removeEventListener('resize', updatePosition);
        resizeObserver.disconnect();
    });

    function handleChange() {
        dispatch("change")
    }

    function handleDelete() {
        dispatch("delete")
    }

    function setItem(item: PresetItem) {
        if (!group.items.some(
            existingItem => JSON.stringify(existingItem) === JSON.stringify(item)
        )) {
            group.items = [...group.items, item];
            handleChange()
        }
    }

    function removeItem(removedId: number) {
        group.items = group.items.filter((_, id) => id != removedId)
        handleChange()
    }

    $: group.items
    $: choiceItems = group.items.filter(it => it.type == "CHOOSE")
            .flatMap(it => it.item);

    $: {
        if (expanded) {
            updatePosition()
        }
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<!-- svelte-ignore a11y-no-noninteractive-element-interactions -->
<div class="container-wrapper">
    <div class="container">
        <div class="items-container">
            <div class="items">
                <div class="add-container" bind:this={addRef}>
                    <div class="item-container add" on:click={() => expanded = !expanded}>
                        <img src="img/menu/icon-plus.svg" alt="Add">
                    </div>

                    {#if expanded}
                        <div
                                class="selector-container-wrapper selector-container"
                                style="
                                    transform: translateX(-50%) scale({$scaleFactor * 50}%);
                                    top: {addElementPosition.top + addElementPosition.height + 15}px;
                                    left: {addElementPosition.left + addElementPosition.width / 2}px
                                "
                                transition:scale={{duration: 200, start: 0.9}}
                                use:clickOutside={() => expanded = false}
                                use:portal
                        >
                            <PresetItemSelector
                                    setItem={setItem}
                                    filter={it => {
                                        console.log(choiceItems)
                                        return !choiceItems.includes(it)
                                    }}
                            />
                        </div>
                    {/if}
                </div>

                {#each group.items as item, idx}
                    <div class="item-background item-container" on:click={() => removeItem(idx)}>
                        <div class="item">
                            <ItemImage bind:item={item} />
                        </div>
                    </div>
                {/each}
            </div>
        </div>
        <div class="slider-wrapper">
            <div class="slider-container">
                <div class="slider-top">
                    <span class="slider-left">Max</span>
                    <div class="slider-right">
                        <ValueInput valueType="int" value={group.stacks}
                                    on:change={(e) => apiSlider.set(e.detail.value)}/>
                        <span>items</span>
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

    .selector-container {
      transform-origin: top;
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

    .container {
      gap: 10px;
      display: flex;
      width: 100%;
    }

    .slider {
      margin-top: 3px;
      padding-right: 10px;
    }

    .items-container {
      border-radius: 6px;
      overflow-y: scroll;
      outline: 1px solid color.adjust($clickgui-text-color, $lightness: -85%);
      max-height: 70px;
      width: 307px;
      flex-shrink: 0;
      background-color: rgba($clickgui-base-color, 0.85);
    }

    .items {
      display: flex;
      gap: 5px;
      padding: 5px;
      flex-wrap: wrap;
    }

    .items-container::-webkit-scrollbar {
      width: 2px;
    }

    .item-container {
      width: 25px;
      height: 25px;
      border-radius: 3px;
      border: none;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: background-color 0.3s ease;

      &:hover {
        background-color: color.adjust($menu-error-color, $lightness: -30%);
      }

      & > .item {
        width: 16px;
        height: 16px;
      }
    }

    .add {
      background: $accent-color;
      position: relative;
      outline: none;
      &:hover {
        background-color: color.adjust(color.adjust($accent-color, $saturation: -30%), $lightness: -10%);
      }
      & > img {
        width: 16px;
        height: 16px;
      }
    }
</style>
