<script lang="ts">
    import Component from "./components/Component.svelte";
    import ComponentList from "./components/ComponentList.svelte";

    interface Props {
        components: Component[]
    }

    let {components}: Props = $props();

    let expanded = $state(true);
</script>

<div class="editor-manager">
    {#if expanded}
        <div class="panel">
            <div class="header">Component Manager</div>
            <ComponentList {components} />
        </div>
    {/if}
    <button class="button-toggle-manager" onclick={() => expanded = !expanded}></button>
</div>

<style lang="scss">
  @use "../../../../colors" as *;

  .header {
    background-color: $hud-editor-manager-header-background-color;
    color: $hud-editor-manager-text-color;
    border-bottom: solid 2px $accent-color;
    padding: 10px 15px;
    font-size: 14px;
    font-weight: 500;
  }

  .editor-manager {
    position: fixed;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    display: flex;
    align-items: center;
  }

  .panel {
    background-color: $hud-editor-manager-content-background-color;
    border-radius: 0 5px 5px 0;
    min-width: 250px;
  }

  .button-toggle-manager {
    background-color: $accent-color;
    padding: 10px;
    cursor: pointer;
    border: none;
    height: 50px;
    width: 25px;
    border-radius: 0 25px 25px 0;
    background-image: url("/img/hud/editor/icon-editor-expand.svg");
    background-repeat: no-repeat;
    background-position: center center;
  }
</style>