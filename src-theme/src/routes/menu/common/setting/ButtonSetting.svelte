<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import CircleLoader from "../CircleLoader.svelte";

    export let title: string;
    export let disabled = false;
    export let secondary = false;
    export let inset = false;
    export let listenForEnter = false;
    export let loading = false;

    const dispatch = createEventDispatcher();

    function handleKeyDown(e: KeyboardEvent) {
        if (!listenForEnter) {
            return;
        }
        if (e.key === "Enter") {
            dispatch("click");
        }
    }
</script>

<svelte:window on:keydown={handleKeyDown}/>
<button class="button-setting" class:inset type="button" on:click={() => dispatch("click")} {disabled} class:secondary>
    {#if loading}
        <CircleLoader/>
    {/if}
    {title}
</button>

<style lang="scss">
  @use "sass:color";
  @use "../../../../colors.scss" as colors;

  .button-setting {
    position: relative;
    border: none;
    background-color: colors.$accent-color;
    color: colors.$menu-text-color;
    font-family: "Inter", sans-serif;
    padding: 20px;
    border-radius: 5px;
    font-size: 20px;
    transition: ease background-color .2s, ease opacity .2s;

    &.inset {
      margin: 0 30px;
    }

    &.secondary {
      background-color: rgba(colors.$menu-base-color, .36);
    }

    &:not([disabled]):hover {
      background-color: color.adjust(colors.$accent-color, $lightness: -10%, $saturation: -30%);
      cursor: pointer;

      &.secondary {
        background-color: color.adjust(colors.$menu-base-color, $lightness: -10%, $saturation: -30%);
      }
    }

    &[disabled] {
      opacity: .6;
    }
  }
</style>
