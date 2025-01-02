<script lang="ts">
    interface Props {
        name: string;
        id: string;
    }

    let {name, id}: Props = $props();
</script>

<div class="component">
    <div class="name">{name}</div>
    <button class="button-delete">Delete</button>
</div>

<style lang="scss">
  @use "../../../../../colors.scss" as *;

  .component {
    display: grid;
    grid-template-columns: 1fr max-content;
    column-gap: 10px;
  }

  .name {
    color: $hud-editor-manager-text-color;
  }
</style>