<script lang="ts">
    import type {ThrowItem} from "../../../../../integration/types";
    import {createEventDispatcher, onDestroy, onMount} from "svelte";
    import {REST_BASE} from "../../../../../integration/host";
    import {scale} from "svelte/transition";
    import {clickOutside, portal} from "../../../../../util/utils";
    import {getRegistries, setTyping} from "../../../../../integration/rest";
    import {scaleFactor} from "../../../clickgui_store";
    import VirtualList from "../../blocks/VirtualList.svelte";

    export let throws: ThrowItem[];
    export let rendered: ThrowItem[] = throws
    export let renderedLength = throws.length

    $: {
        renderedLength = rendered.length
    }

    const dispatch = createEventDispatcher();

    interface TItem {
        name: string;
        identifier: string;
    }

    let items: TItem[] = [];
    let renderedItems: TItem[] = items;

    let expanded = false;
    let searchQuery = "";

    let addRef: HTMLElement;
    let addElementPosition = { top: 0, left: 0, width: 0, height: 0 };


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

    const resizeObserver = new ResizeObserver(updatePosition);
    onMount(() => {
        window.addEventListener('resize', updatePosition);
        if (addRef) resizeObserver.observe(addRef);
        updatePosition();
    });

    onDestroy(() => {
        window.removeEventListener('resize', updatePosition);
        resizeObserver.disconnect();
    });

    $: {
        dispatch('renderedUpdate', rendered.length)
    }

    $: {
        if (expanded) {
            updatePosition()
        }
    }

    $: {
        let filteredItems = items;

        if (searchQuery) {
            filteredItems = filteredItems.filter(b => b.name.toLowerCase().includes(searchQuery.toLowerCase()));
        }

        filteredItems = filteredItems.filter(b => !rendered.includes(b.identifier) && b.identifier !== "minecraft:air")

        renderedItems = filteredItems;
    }

    onMount(async () => {
        let registries = (await getRegistries()).items;

        if (registries !== undefined) {
            items = registries.sort((a, b) => a.identifier.localeCompare(b.identifier));
        }
    });

    function handleAdd(item: ThrowItem) {
        if (rendered.includes(item)) {
            return
        }

        rendered = [...rendered, item]
    }

    function handleRemove(item: ThrowItem) {
        rendered = rendered.filter((e) => e != item)
    }

    export function clear() {
        rendered = []
        flush()
    }

    export function flush() {
        if (JSON.stringify(throws) === JSON.stringify(rendered)) {
            return
        }

        throws = [...rendered]
        dispatch("change")
    }
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="items-container">
    <div class="items">
        <div class="add-container" bind:this={addRef}>
            <div
                    class="item-container add"
                    on:click={() => expanded = !expanded}
            >
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
                    <div class="select-selector">
                        <div class="select-title">Search</div>

                        <div class="search-wrapper">
                            <div class="search">
                                <input
                                        type="text"
                                        placeholder="Search items..."
                                        class="search-input"
                                        bind:value={searchQuery}
                                        on:focusin={async () => await setTyping(true)}
                                        on:focusout={async () => await setTyping(false)}
                                        spellcheck="false">
                                <div class="search-icon">
                                    <img src="img/menu/icon-pen.svg" alt="Search" />
                                </div>
                            </div>
                        </div>

                        {#if renderedItems.length > 0}
                            <div class="results">
                                <VirtualList items={renderedItems} let:item>
                                    <div class="result-item" on:click={() => handleAdd(item.identifier)}>
                                        <div class="icon-wrapper">
                                            <img class="icon" src="{REST_BASE}/api/v1/client/resource/itemTexture?id={item.identifier}" alt={item.identifier}/>
                                        </div>

                                        <span class="name">
                                        {item.name}
                                    </span>
                                    </div>
                                </VirtualList>
                            </div>
                        {:else}
                            <span class="items-group-title">No Results</span>
                        {/if}
                    </div>
                </div>
            {/if}
        </div>

        {#each rendered as item}
            <div class="item-container" on:click={() => handleRemove(item)}>
                <div class="item">
                    <img src="{REST_BASE}/api/v1/client/resource/itemTexture?id={item}" alt={item}/>
                </div>
            </div>
        {/each}
    </div>
</div>

<style lang="scss">
  @use "sass:color";
  @use "../../../../../colors.scss" as *;
  @use "select" as *;

  .items-container {
    border-radius: 6px;
    overflow: hidden;
    outline: 1px solid color.adjust($clickgui-text-color, $lightness: -85%);
    background-color: rgba($clickgui-base-color, 0.85);
  }

  .items {
    overflow-y: scroll;
    max-height: 184px;
    width: 100%;
    display: flex;
    gap: 11px;
    padding: 12px;
    flex-wrap: wrap;
  }

  .items::-webkit-scrollbar {
    width: 2px;
  }

  .item-container {
    width: 32px;
    height: 32px;
    border-radius: 3px;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: color.adjust($clickgui-text-color, $lightness: -90%);
    cursor: pointer;
  }

  .add-container {
    position: relative;
  }

  .item {
    & > img {
      width: 25px;
      height: 25px;
    }
  }

  .selector-container {
    transform-origin: top;
  }

  .add {
    background: $accent-color;
    position: relative;

    &:hover {
      background-color: color.adjust(color.adjust($accent-color, $saturation: -30%), $lightness: -10%);
    }

    & > img {
      width: 16px;
      height: 16px;
    }
  }
</style>
