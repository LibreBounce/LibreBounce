<div class="button-container">
    <slot />
</div>

<style lang="scss">
    @use "../../../../colors.scss" as colors;

    .button-container {
      background-color: rgba(colors.$menu-base-color, 0.68);
      padding: 15px 30px;
      width: max-content;
      border-radius: 5px;
      display: flex;
      column-gap: 20px;
    }
</style>
