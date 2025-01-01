<script lang="ts">
    import {type Alignment, HorizontalAlignment, VerticalAlignment} from "../../../integration/types.js";
    import {scaleFactor} from "../../clickgui/clickgui_store";
    import {moveComponent} from "../../../integration/rest";
    import {onMount} from "svelte";
    import ComponentSettings from "./ComponentSettings.svelte";

    export let alignment: Alignment;
    export let id: string;
    export let name: string;
    export let editorMode: boolean;

    // TODO: make configurable
    let gridSize = 10;
    let snappingEnabled = true;

    let element: HTMLElement | undefined;

    let moving = false;
    let ignoreGrid = false;
    let offsetX = 0;
    let offsetY = 0;

    let settingsBottom = false;

    function updateSettingsBottom() {
        settingsBottom = (element?.offsetTop ?? 0) < window.innerHeight / 2 - (element?.offsetHeight ?? 0) / 2;
    }

    $: styleString = generateStyleString(alignment);

    function clamp(number: number, min: number, max: number) {
        return Math.max(min, Math.min(number, max));
    }

    function onMouseDown(e: MouseEvent) {
        if (e.button !== 0 && e.button !== 1) return;

        moving = true;
        if (alignment.horizontal !== HorizontalAlignment.RIGHT) {
            offsetX = e.clientX * (2 / $scaleFactor) - alignment.horizontalOffset;
        } else {
            offsetX = e.clientX * (2 / $scaleFactor) + alignment.horizontalOffset;
        }

        if (alignment.vertical !== VerticalAlignment.BOTTOM) {
            offsetY = e.clientY * (2 / $scaleFactor) - alignment.verticalOffset;
        } else {
            offsetY = e.clientY * (2 / $scaleFactor) + alignment.verticalOffset;
        }
    }

    function onMouseMove(e: MouseEvent) {
        if (!moving) {
            return;
        }

        let newHorizontal = 0;
        let newVertical = 0;

        if (alignment.horizontal !== HorizontalAlignment.RIGHT) {
            newHorizontal = e.clientX * (2 / $scaleFactor) - offsetX;
        } else {
            newHorizontal = offsetX - e.clientX * (2 / $scaleFactor);
        }

        switch (alignment.horizontal) {
            case HorizontalAlignment.CENTER_TRANSLATED:
                newHorizontal = clamp(
                    newHorizontal,
                    -window.innerWidth / 2 + (element?.offsetWidth ?? 0) / 2,
                    window.innerWidth / 2 - (element?.offsetWidth ?? 0) / 2
                );
                break;
            case HorizontalAlignment.CENTER:
                newHorizontal = clamp(
                    newHorizontal,
                    -window.innerWidth / 2,
                    window.innerWidth / 2 - (element?.offsetWidth ?? 0)
                );
                break;
            case HorizontalAlignment.LEFT:
            case HorizontalAlignment.RIGHT:
                newHorizontal = clamp(newHorizontal, 0, window.innerWidth - (element?.offsetWidth ?? 0));
                break;
        }

        if (alignment.vertical !== VerticalAlignment.BOTTOM) {
            newVertical = (e.clientY * (2 / $scaleFactor) - offsetY);
        } else {
            newVertical = offsetY - (e.clientY * (2 / $scaleFactor));
        }

        switch (alignment.vertical) {
            case VerticalAlignment.CENTER_TRANSLATED:
                newVertical = clamp(
                    newVertical,
                    -window.innerHeight / 2 + (element?.offsetHeight ?? 0) / 2,
                    window.innerHeight / 2 - (element?.offsetHeight ?? 0) / 2
                );
                break;
            case VerticalAlignment.CENTER:
                newVertical = clamp(
                    newVertical,
                    -window.innerHeight / 2,
                    window.innerHeight / 2 - (element?.offsetHeight ?? 0)
                );
                break;
            case VerticalAlignment.TOP:
            case VerticalAlignment.BOTTOM:
                newVertical = clamp(newVertical, 0, window.innerHeight - (element?.offsetHeight ?? 0));
                break;
        }

        alignment.horizontalOffset = snapToGrid(newHorizontal);
        alignment.verticalOffset = snapToGrid(newVertical);

        updateSettingsBottom();
    }

    function snapToGrid(value: number): number {
        if (ignoreGrid || !snappingEnabled) return value;

        return Math.round(value / gridSize) * gridSize;
    }

    async function onMouseUp() {
        moving = false;
        await moveComponent(id, alignment);
    }

    function generateStyleString(alignment: Alignment): string {
        let style = "position: fixed;";

        switch (alignment.horizontal) {
            case HorizontalAlignment.LEFT:
                style += `left: ${alignment.horizontalOffset}px;`;
                break;
            case HorizontalAlignment.RIGHT:
                style += `right: ${alignment.horizontalOffset}px;`;
                break;
            case HorizontalAlignment.CENTER:
            case HorizontalAlignment.CENTER_TRANSLATED:
                style += `left: calc(50% + ${alignment.horizontalOffset}px);`;
                break;
        }

        switch (alignment.vertical) {
            case VerticalAlignment.TOP:
                style += `top: ${alignment.verticalOffset}px;`;
                break;
            case VerticalAlignment.BOTTOM:
                style += `bottom: ${alignment.verticalOffset}px;`;
                break;
            case VerticalAlignment.CENTER:
            case VerticalAlignment.CENTER_TRANSLATED:
                style += `top: calc(50% + ${alignment.verticalOffset}px);`;
                break;
        }

        style += "transform: translate("
        if (alignment.horizontal === HorizontalAlignment.CENTER_TRANSLATED) {
            style += "-50%,";
        } else {
            style += "0,";
        }
        if (alignment.vertical === VerticalAlignment.CENTER_TRANSLATED) {
            style += "-50%);";
        } else {
            style += "0);"
        }

        return style;
    }

    function handleKeydown(e: KeyboardEvent) {
        if (e.key === "Shift") {
            ignoreGrid = true;
        }
    }

    function handleKeyup(e: KeyboardEvent) {
        if (e.key === "Shift") {
            ignoreGrid = false;
        }
    }

    onMount(() => {
        updateSettingsBottom();
    });
</script>

<svelte:window on:mouseup={onMouseUp} on:mousemove={onMouseMove} on:keydown={handleKeydown} on:keyup={handleKeyup}/>

<div class="draggable-element" style={styleString} bind:this={element}>
    <!-- svelte-ignore a11y-no-static-element-interactions -->
    <div class="contained-element" on:mousedown={onMouseDown} class:editor-mode={editorMode}>
        <slot/>
    </div>
    {#if editorMode}
        <ComponentSettings horizontalOffset={alignment.horizontalOffset} {name} {id} bottom={settingsBottom}/>
    {/if}
</div>

<style lang="scss">
  @import "../../../colors";

  .draggable-element {
    position: relative;
  }

  .contained-element {
    min-width: 50px;
    min-height: 50px;
  }

  .editor-mode {
    border: solid 1px $hud-editor-element-border-color;
    background-color: $hud-editor-element-background-color;
  }
</style>