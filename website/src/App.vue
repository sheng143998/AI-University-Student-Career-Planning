<script setup lang="ts"></script>

<template>
  <RouterView v-slot="{ Component, route }">
    <Transition name="warp-route" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </Transition>
  </RouterView>
</template>

<style>
.warp-route-enter-active,
.warp-route-leave-active {
  transition:
    opacity 520ms ease,
    transform 640ms cubic-bezier(0.2, 0.8, 0.2, 1),
    filter 520ms ease;
}

.warp-route-enter-from {
  opacity: 0;
  transform: scale(1.06) translate3d(0, 10px, 0);
  filter: blur(8px) saturate(1.25);
}

.warp-route-enter-to {
  opacity: 1;
  transform: scale(1) translate3d(0, 0, 0);
  filter: blur(0) saturate(1);
}

.warp-route-leave-from {
  opacity: 1;
  transform: scale(1) translate3d(0, 0, 0);
  filter: blur(0) saturate(1);
}

.warp-route-leave-to {
  opacity: 0;
  transform: scale(0.965) translate3d(0, -8px, 0);
  filter: blur(6px) saturate(1.15);
}

@media (prefers-reduced-motion: reduce) {
  .warp-route-enter-active,
  .warp-route-leave-active {
    transition: none;
  }
}
</style>
