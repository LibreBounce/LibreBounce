<script lang="ts">
    export let color: string;
    export let radius: number;
    export let rotateX: number;
    export let rotateZ: number;
</script>

<div class="pointer" style="
    transform-origin: 50% {radius}px;
    transform: translateY(-{radius}px) rotateX({rotateX}deg) rotateZ({rotateZ}deg);
    border-bottom: 16px solid {color};
"></div>

<style lang="scss">
  .pointer {
    width: 0;
    height: 0;
    position: absolute;
    border-left: 5px solid transparent;
    border-right: 5px solid transparent;
  }
</style>
