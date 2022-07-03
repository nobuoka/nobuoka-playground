<template>
  <div class="sample-component">
    <span>{{ msg }}</span>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'SampleComponent',
  props: {
    msg: String,
  },
});
</script>

<style scoped>
</style>
