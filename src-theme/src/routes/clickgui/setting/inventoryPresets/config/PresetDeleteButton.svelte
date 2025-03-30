<script lang="ts">
    import {clickOutside} from "../../../../../util/utils";
    import {createEventDispatcher} from "svelte";

    const dispatch = createEventDispatcher();

    let stage: number = 0
    let timeoutId: ReturnType<typeof setTimeout>;

    const stages: string[] = [
        "Delete",
        "Are you sure?",
        "ARE YOU SURE?"
    ]

    $: {
        if (stage >= stages.length) {
            dispatch("delete")
            resetTimer()
        } else if (stage !== 0) {
            resetTimer()
        }
    }

    function resetTimer() {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(resetState, 2000);
    }

    function resetState() {
        stage = 0;
        clearTimeout(timeoutId);
    }

</script>

<button on:click={() => stage++ } use:clickOutside={() => stage = 0} {...$$props}>
    {stages[stage]}
</button>

<style lang="scss">
  @use "../../../../../colors.scss" as *;

  button {
    cursor: pointer;
    border: none;
    padding: 5px 15px;
    color: $menu-text-color;
    border-radius: 3px;

    background: $menu-error-color !important;

    &:hover {
      background: rgba($menu-error-color, 0.9) !important;
    }
  }
</style>
