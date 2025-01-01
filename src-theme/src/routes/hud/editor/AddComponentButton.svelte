<script lang="ts">

    import {createComponent} from "../../../integration/rest";
    import {THEME_NAME} from "../../../integration/host";

    async function handleClick() {
        await createComponent(THEME_NAME, "Effects");
    }
</script>

<button on:click={handleClick} class="button-add-component">Add Component</button>

<style lang="scss">
    .button-add-component {
      position: fixed;
      bottom: 20px;
      left: 20px;
    }
</style>