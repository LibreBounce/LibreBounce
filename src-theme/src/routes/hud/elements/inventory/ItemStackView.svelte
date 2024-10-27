<script lang="ts">
    import type {ItemStack} from "../../../../integration/types";
    import {REST_BASE} from "../../../../integration/host";
    import {effectFrame} from "./inventory";

    export let stack: ItemStack;

    const {count, damage, identifier, maxDamage, hasEnchantment} = stack;

    const countColor = count <= 0 ? 'red' : 'white';

    const valueColor = (() => {
        const value = 120 * (maxDamage - damage) / maxDamage;
        if (value <= 0) {
            return 'rgb(255, 0, 0)';
        } else if (value <= 60) {
            return `rgb(255, ${Math.floor(value * 255 / 60)}, 0)`;
        } else if (value <= 120) {
            return `rgb(${Math.floor((120 - value) * 255 / 60)}, 255, 0)`;
        } else {
            return 'rgb(0, 255, 0)';
        }
    })();

    const imgUrl = REST_BASE + '/api/v1/client/resource/itemTexture?id=' + identifier;
</script>

<figure class="item-stack">
    {#if hasEnchantment}
        <img class="mask" style="mask-size: cover; mask-image: url({imgUrl})" src={$effectFrame} alt="Mask-{identifier}">
    {/if}
    <img class="icon" src={imgUrl} alt={identifier}/>

    <div class="durability-bar" class:hidden={damage === 0}>
        <div class="durability"
             style="width: {100 * (maxDamage - damage) / maxDamage}%; background-color: {valueColor}">
        </div>
    </div>

    <div class="count" class:hidden={count === 0 || count === 1} style="color: {countColor}">
        {count}
    </div>
</figure>

<style lang="scss">
  @import "../../../../colors";

  .hidden {
    display: none;
  }

  .item-stack {
    position: relative;
    width: 32px;
    height: 32px;
  }

  .mask {
    position: absolute;
    opacity: 50%; // TODO
    filter: brightness(5); // TODO
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
  }

  .icon {
    width: 100%;
    height: 100%;
  }

  .durability-bar {
    position: absolute;
    bottom: 0;
    left: 10%;
    width: 80%;
    height: 2px;
    background-color: rgba($item-damage-base-color, 0.68);
  }

  .durability {
    height: 100%;
    transition: width 150ms;
  }

  .count {
    position: absolute;
    bottom: 0;
    right: 0;
    font-size: 14px;
    font-weight: bold;
    text-shadow: 1px 1px black;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  }
</style>
