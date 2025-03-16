<script lang="ts">
    import {convertToSpacedString, spaceSeperatedNames} from "../../../../../theme/theme_config";

    export let value: string | string[];
    export let options: string[];

    $: values = (Array.isArray(value) ? value : [value])
        .slice()
        .sort((a, b) => {
            return options.indexOf(a) - options.indexOf(b);
        });

</script>

<span class="text">
    {#if values.length > 0}
        {#each values as v, i}
            {$spaceSeperatedNames ? convertToSpacedString(v) : v}{i < values.length - 1 ? ', ' : ''}
        {/each}
    {:else}
        None
    {/if}
</span>
