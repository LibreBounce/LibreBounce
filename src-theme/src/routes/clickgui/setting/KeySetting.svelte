<script lang="ts">
    import type {KeySetting, ModuleSetting} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import {getPrintableKeyName} from "../../../integration/rest";
    import {createEventDispatcher} from "svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as KeySetting;

    const dispatch = createEventDispatcher();
    const UNKNOWN_KEY = -1;

    let binding = false;
    let printableKeyName = "";

    $: {
        if (cSetting.value !== -1) {
            getPrintableKeyName(cSetting.value) // TODO: Implement
                .then(printableKey => {
                    printableKeyName = printableKey.localized;
                });
        }
    }

    async function toggleBinding() {
        if (binding) {
            cSetting.value = UNKNOWN_KEY;
        }

        binding = !binding;

        setting = {...cSetting};

        dispatch("change");
    }
</script>

<div class="setting">
    <button class="change-bind" on:click={toggleBinding}>
        {#if !binding}
            <div class="name">{$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}:</div>

            {#if cSetting.value === UNKNOWN_KEY}
                <span class="none">None</span>
            {:else}
                <span>{printableKeyName}</span>
            {/if}
        {:else}
            <span>Press any key</span>
        {/if}
    </button>
</div>