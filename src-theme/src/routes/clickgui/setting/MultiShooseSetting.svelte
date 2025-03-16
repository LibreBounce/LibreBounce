<script lang="ts">
    import {createEventDispatcher} from "svelte";
    import type {ModuleSetting, MultiChooseSetting,} from "../../../integration/types";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";
    import Dropdown from "./common/dropdown/Dropdown.svelte";

    export let setting: ModuleSetting;

    const cSetting = setting as MultiChooseSetting;

    const dispatch = createEventDispatcher();

    function handleChange() {
        setting = { ...cSetting };
        dispatch("change");
    }
</script>

<div class="setting">
    <Dropdown
        on:change={handleChange}
        bind:value={cSetting.value}
        options={cSetting.choices}
        name={$spaceSeperatedNames ? convertToSpacedString(cSetting.name) : cSetting.name}
    />
</div>

<style lang="scss">
    .setting {
        padding: 7px 0;
    }
</style>
