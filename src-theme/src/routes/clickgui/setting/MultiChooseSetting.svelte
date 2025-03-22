<script lang="ts">
    import {createEventDispatcher, onMount} from "svelte";
    import type {ModuleSetting, MultiChooseSetting,} from "../../../integration/types";
    import {slide} from "svelte/transition";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import ExpandArrow from "./common/ExpandArrow.svelte";
    import {setItem} from "../../../integration/persistent_storage";

    export let setting: ModuleSetting;
    export let path: string;

    const cSetting = setting as MultiChooseSetting;
    const thisPath = `${path}.${cSetting.name}`;

    const dispatch = createEventDispatcher();

    function handleChange(v: string) {
        if (cSetting.value.includes(v)) {
            const filtered = cSetting.value.filter(item => item !== v);

            if (filtered.length === 0 && !cSetting.canBeNone) {
                // Doesn't remove the element because in this case value will be empty
                return;
            }

            cSetting.value = filtered;
        } else {
            cSetting.value = [...cSetting.value, v]
        }

        setting = { ...cSetting };
        dispatch("change");
    }

    let expanded = localStorage.getItem(thisPath) === "true";
    let skipAnimationDelay = false;

    $: setItem(thisPath, expanded.toString());

    function toggleExpanded() {
        expanded = !expanded;
        skipAnimationDelay = true;
    }

    onMount(() => {
        setTimeout(() => {
            skipAnimationDelay = true;
        }, 200)
    });
</script>

<!-- svelte-ignore a11y-click-events-have-key-events -->
<!-- svelte-ignore a11y-no-static-element-interactions -->
<div class="setting">
    <div class="head" class:expanded on:contextmenu|preventDefault={toggleExpanded} on:click|preventDefault={toggleExpanded}>
        <div class="title">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}</div>
        <div class="right">
            <span>
                <span class="value">{cSetting.value.length}</span>
                /
                <span class="value">{cSetting.choices.length}</span>
            </span>
            <ExpandArrow bind:expanded on:click={() => skipAnimationDelay = true} />
        </div>
    </div>

    {#if expanded && skipAnimationDelay}
        <div in:slide|global={{duration: 200, axis: "y"}} out:slide|global={{duration: 200, axis: "y"}} class="choices">
            {#each cSetting.choices as choice}
                <span
                        class="choice"
                        class:active={cSetting.value.includes(choice)}
                        on:click={() => {
                            handleChange(choice)
                        }}
                >
                    {$spaceSeperatedNames ? convertToSpacedString(choice) : choice}
                </span>
            {/each}
        </div>
    {/if}
</div>

<style lang="scss">
  @use "../../../colors.scss" as *;

  .setting {
    padding: 7px 0;
    color: $clickgui-text-color;
  }

  .title {
    color: $clickgui-text-color;
    font-size: 12px;
    font-weight: 600;
  }

  .choice {
    color: $clickgui-text-dimmed-color;
    background-color: rgba($clickgui-base-color, 0.3);
    border-radius: 3px;
    padding: 3px 6px;
    cursor: pointer;
    font-weight: 500;
    transition: ease color 0.2s;
    overflow-wrap: anywhere;

    &:hover {
      color: $clickgui-text-color;
    }

    &.active {
      background-color: rgba($accent-color, 0.1);
      color: $accent-color;
    }
  }

  .right {
    display: flex;
    align-items: center;

    & > span {
      letter-spacing: 1px;
      font-weight: 500;
      font-size: 12px;
      border: none;
    }

    & > span > .value {
      font-family: monospace;
      line-height: 0;
    }
  }

  .head {
    display: flex;
    justify-content: space-between;
    transition: ease margin-bottom .2s;

    &.expanded {
      margin-bottom: 10px;
    }
  }

  .choices {
    border-left: solid 2px $accent-color;
    color: $clickgui-text-color;
    padding: 0 7px;
    display: flex;
    flex-wrap: wrap;
    gap: 7px;
    font-size: 12px;
  }
</style>
