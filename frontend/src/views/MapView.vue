<template>
  <section class="view page map-page">
    <GeoNearbyMap />
  </section>
</template>

<script setup>
import GeoNearbyMap from '../components/GeoNearbyMap.vue'
</script>

<style scoped>
.map-page {
  display: block;
}
</style>
